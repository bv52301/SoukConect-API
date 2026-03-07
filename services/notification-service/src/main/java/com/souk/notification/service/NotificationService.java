package com.souk.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.souk.notification.dto.*;
import com.souk.notification.model.NotificationRequest;
import com.souk.notification.repository.NotificationRequestRepository;
import com.souk.notification.service.channel.ChannelSender;
import com.souk.notification.service.channel.EmailChannelSender;
import com.souk.notification.service.channel.PushChannelSender;
import com.souk.notification.service.channel.SmsChannelSender;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRequestRepository requestRepository;
    private final WorkflowClient workflowClient;
    private final ObjectMapper objectMapper;
    private final PushChannelSender pushSender;
    private final EmailChannelSender emailSender;
    private final SmsChannelSender smsSender;

    public NotificationService(NotificationRequestRepository requestRepository,
                               WorkflowClient workflowClient,
                               ObjectMapper objectMapper,
                               PushChannelSender pushSender,
                               EmailChannelSender emailSender,
                               SmsChannelSender smsSender) {
        this.requestRepository = requestRepository;
        this.workflowClient = workflowClient;
        this.objectMapper = objectMapper;
        this.pushSender = pushSender;
        this.emailSender = emailSender;
        this.smsSender = smsSender;
    }

    /**
     * Task 5.8 — Idempotency: check notificationId before processing.
     * Returns true if already processed (caller should return 202 without re-processing).
     */
    @Transactional
    public boolean isAlreadyProcessed(String notificationId) {
        return requestRepository.existsById(notificationId);
    }

    /**
     * Task 5.7 — Receive request, store for idempotency, then dispatch asynchronously.
     */
    @Transactional
    public void accept(NotificationServiceRequest request) {
        NotificationRequest entity = new NotificationRequest();
        entity.setNotificationId(request.notificationId());
        entity.setWorkflowId(request.workflowId());
        entity.setRecipientId(request.recipientId());
        entity.setRecipientType(request.recipientType());
        entity.setStatus("ACCEPTED");
        entity.setSignalSent(false);
        requestRepository.save(entity);
    }

    /**
     * Task 5.9 / 5.10 — Execute dispatch strategy and signal BPM with results.
     * Runs on a background thread (called from controller with @Async).
     */
    @Async
    public void process(NotificationServiceRequest request) {
        List<DeliveryResult> results;
        try {
            if (request.dispatchStrategy() == DispatchStrategy.BROADCAST) {
                results = broadcast(request);
            } else {
                results = sequentialFallback(request);
            }
        } catch (Exception e) {
            log.error("Dispatch failed for notificationId={}: {}", request.notificationId(), e.getMessage(), e);
            results = List.of(new DeliveryResult(null, DeliveryStatus.FAILED, e.getMessage()));
        }

        persistResultsAndSignal(request.notificationId(), request.workflowId(), results);
    }

    /**
     * Task 5.9 — SEQUENTIAL_FALLBACK: attempt channels in order; stop on first SENT.
     */
    private List<DeliveryResult> sequentialFallback(NotificationServiceRequest request) {
        List<DeliveryResult> results = new ArrayList<>();
        for (NotificationChannel channel : request.channels()) {
            if (isOptedOut(request, channel)) {
                results.add(new DeliveryResult(channel, DeliveryStatus.FAILED, "Recipient opted out"));
                continue;
            }
            DeliveryResult result = getSender(channel).send(request);
            results.add(result);
            if (result.status() == DeliveryStatus.SENT) {
                break; // stop on first success
            }
        }
        return results;
    }

    /**
     * Task 5.10 — BROADCAST: send all channels concurrently; collect all results.
     */
    private List<DeliveryResult> broadcast(NotificationServiceRequest request) {
        List<CompletableFuture<DeliveryResult>> futures = request.channels().stream()
                .map(channel -> CompletableFuture.supplyAsync(() -> {
                    if (isOptedOut(request, channel)) {
                        return new DeliveryResult(channel, DeliveryStatus.FAILED, "Recipient opted out");
                    }
                    return getSender(channel).send(request);
                }))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return futures.stream().map(CompletableFuture::join).toList();
    }

    /**
     * Task 5.15 — Persist results to DB before signalling; then signal BPM.
     */
    @Transactional
    public void persistResultsAndSignal(String notificationId, String workflowId, List<DeliveryResult> results) {
        // Persist delivery results first so we can retry on startup if signal fails
        Optional<NotificationRequest> entityOpt = requestRepository.findById(notificationId);
        if (entityOpt.isPresent()) {
            NotificationRequest entity = entityOpt.get();
            try {
                entity.setDeliveryResultsJson(objectMapper.writeValueAsString(results));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize delivery results for notificationId={}", notificationId, e);
            }
            entity.setStatus("COMPLETED");
            requestRepository.save(entity);
        }

        // Task 5.14 — Signal BPM: deliveryComplete(results)
        sendSignal(workflowId, results);

        // Mark signal sent
        entityOpt.ifPresent(entity -> {
            entity.setSignalSent(true);
            requestRepository.save(entity);
        });
    }

    /**
     * Task 5.14 — Signal the NotificationWorkflow with delivery results via Temporal SDK.
     */
    private void sendSignal(String workflowId, List<DeliveryResult> results) {
        try {
            WorkflowStub stub = workflowClient.newUntypedWorkflowStub(workflowId);
            stub.signal("deliveryComplete", results);
            log.info("Signalled deliveryComplete to workflowId={} with {} results", workflowId, results.size());
        } catch (Exception e) {
            log.error("Failed to signal workflowId={}: {}", workflowId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Task 5.15 — Retry pending signals on startup for requests where signal was not sent.
     */
    @Transactional
    public void retryPendingSignals() {
        List<NotificationRequest> pending = requestRepository.findBySignalSentFalseAndStatusNotNull();
        if (pending.isEmpty()) return;
        log.info("Retrying {} pending notification signals on startup", pending.size());
        for (NotificationRequest entity : pending) {
            try {
                List<DeliveryResult> results = parseResults(entity.getDeliveryResultsJson());
                sendSignal(entity.getWorkflowId(), results);
                entity.setSignalSent(true);
                requestRepository.save(entity);
            } catch (Exception e) {
                log.warn("Signal retry failed for notificationId={}: {}", entity.getNotificationId(), e.getMessage());
            }
        }
    }

    private List<DeliveryResult> parseResults(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse delivery results JSON: {}", e.getMessage());
            return List.of();
        }
    }

    private ChannelSender getSender(NotificationChannel channel) {
        return switch (channel) {
            case PUSH -> pushSender;
            case EMAIL -> emailSender;
            case SMS -> smsSender;
        };
    }

    private boolean isOptedOut(NotificationServiceRequest request, NotificationChannel channel) {
        return request.recipientPreferences() != null
                && request.recipientPreferences().optOuts() != null
                && request.recipientPreferences().optOuts().contains(channel);
    }
}
