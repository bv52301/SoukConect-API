package com.souk.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.souk.notification.dto.*;
import com.souk.notification.model.NotificationRequest;
import com.souk.notification.repository.NotificationRequestRepository;
import com.souk.notification.service.channel.EmailChannelSender;
import com.souk.notification.service.channel.PushChannelSender;
import com.souk.notification.service.channel.SmsChannelSender;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRequestRepository requestRepository;
    @Mock WorkflowClient workflowClient;
    @Mock WorkflowStub workflowStub;
    @Mock PushChannelSender pushSender;
    @Mock EmailChannelSender emailSender;
    @Mock SmsChannelSender smsSender;

    NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(requestRepository, workflowClient,
                new ObjectMapper(), pushSender, emailSender, smsSender);
        lenient().when(workflowClient.newUntypedWorkflowStub(anyString())).thenReturn(workflowStub);
        // Default: entity is found for signal tests
        NotificationRequest entity = new NotificationRequest();
        entity.setNotificationId("notif-1");
        entity.setWorkflowId("notify-notif-1");
        lenient().when(requestRepository.findById("notif-1")).thenReturn(Optional.of(entity));
        lenient().when(requestRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    /**
     * Task 9.6 — SEQUENTIAL_FALLBACK stops on first SENT
     */
    @Test
    void sequentialFallback_stopsOnFirstSent() {
        when(pushSender.send(any())).thenReturn(new DeliveryResult(NotificationChannel.PUSH, DeliveryStatus.SENT, null));

        NotificationServiceRequest request = buildRequest(DispatchStrategy.SEQUENTIAL_FALLBACK,
                List.of(NotificationChannel.PUSH, NotificationChannel.EMAIL));
        service.process(request);

        verify(pushSender).send(any());
        verify(emailSender, never()).send(any());
        ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
        verify(workflowStub).signal(eq("deliveryComplete"), captor.capture());
        @SuppressWarnings("unchecked")
        List<DeliveryResult> results = (List<DeliveryResult>) captor.getValue()[0];
        assertThat(results).hasSize(1);
        assertThat(results.get(0).status()).isEqualTo(DeliveryStatus.SENT);
    }

    /**
     * Task 9.7 — SEQUENTIAL_FALLBACK continues on FAILED; collects all attempted results
     */
    @Test
    void sequentialFallback_continuesOnFailedAndCollectsResults() {
        when(pushSender.send(any())).thenReturn(new DeliveryResult(NotificationChannel.PUSH, DeliveryStatus.FAILED, "no token"));
        when(emailSender.send(any())).thenReturn(new DeliveryResult(NotificationChannel.EMAIL, DeliveryStatus.SENT, null));

        NotificationServiceRequest request = buildRequest(DispatchStrategy.SEQUENTIAL_FALLBACK,
                List.of(NotificationChannel.PUSH, NotificationChannel.EMAIL));
        service.process(request);

        verify(pushSender).send(any());
        verify(emailSender).send(any());
        ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
        verify(workflowStub).signal(eq("deliveryComplete"), captor.capture());
        @SuppressWarnings("unchecked")
        List<DeliveryResult> results = (List<DeliveryResult>) captor.getValue()[0];
        assertThat(results).hasSize(2);
    }

    /**
     * Task 9.8 — BROADCAST dispatches all channels concurrently; collects all results
     */
    @Test
    void broadcast_dispatchesAllChannelsAndCollectsAllResults() {
        when(pushSender.send(any())).thenReturn(new DeliveryResult(NotificationChannel.PUSH, DeliveryStatus.SENT, null));
        when(emailSender.send(any())).thenReturn(new DeliveryResult(NotificationChannel.EMAIL, DeliveryStatus.SENT, null));
        when(smsSender.send(any())).thenReturn(new DeliveryResult(NotificationChannel.SMS, DeliveryStatus.FAILED, "err"));

        NotificationServiceRequest request = buildRequest(DispatchStrategy.BROADCAST,
                List.of(NotificationChannel.PUSH, NotificationChannel.EMAIL, NotificationChannel.SMS));
        service.process(request);

        verify(pushSender).send(any());
        verify(emailSender).send(any());
        verify(smsSender).send(any());
        ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
        verify(workflowStub).signal(eq("deliveryComplete"), captor.capture());
        @SuppressWarnings("unchecked")
        List<DeliveryResult> results = (List<DeliveryResult>) captor.getValue()[0];
        assertThat(results).hasSize(3);
    }

    /**
     * Task 9.9 — Duplicate notificationId returns 202 without re-processing
     */
    @Test
    void duplicateNotificationId_isAlreadyProcessed() {
        when(requestRepository.existsById("notif-1")).thenReturn(true);
        assertThat(service.isAlreadyProcessed("notif-1")).isTrue();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private NotificationServiceRequest buildRequest(DispatchStrategy strategy, List<NotificationChannel> channels) {
        return new NotificationServiceRequest(
                "notif-1", "notify-notif-1", "cust-1", "CUSTOMER",
                channels, strategy, NotificationPriority.NORMAL,
                "Subject", "Title", "Body",
                new RecipientPreferences(List.of(), "a@b.com", "+123", "en", List.of()));
    }
}
