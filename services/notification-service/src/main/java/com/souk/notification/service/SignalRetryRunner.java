package com.souk.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Task 5.15 — On startup, retry any delivery results that were persisted but not yet signalled to BPM.
 * Handles the case where the service crashed after delivery but before the signal was sent.
 */
@Component
public class SignalRetryRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SignalRetryRunner.class);

    private final NotificationService notificationService;

    public SignalRetryRunner(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Checking for pending notification signals to retry...");
        try {
            notificationService.retryPendingSignals();
        } catch (Exception e) {
            log.warn("Signal retry runner failed (non-fatal): {}", e.getMessage());
        }
    }
}
