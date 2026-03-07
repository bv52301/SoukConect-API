package com.souk.customer.api;

import com.souk.common.adapters.jpa.repository.CustomerRepository;
import com.souk.common.adapters.jpa.repository.DeviceTokenRepository;
import com.souk.common.domain.Customer;
import com.souk.common.domain.DeviceToken;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Task 6.3 — GET /customers/{id}/device-tokens
 * Task 6.5 — POST /device-tokens/register
 * Task 6.6 — DELETE /device-tokens/{token}
 */
@RestController
public class DeviceTokenController {

    private final CustomerRepository customerRepository;
    private final DeviceTokenRepository deviceTokenRepository;

    public DeviceTokenController(CustomerRepository customerRepository,
                                 DeviceTokenRepository deviceTokenRepository) {
        this.customerRepository = customerRepository;
        this.deviceTokenRepository = deviceTokenRepository;
    }

    /**
     * Task 6.3 — Returns active device tokens for a customer.
     * Used by OrderOrchestrationWorkflow to populate RecipientPreferences.deviceTokens.
     */
    @GetMapping("/customers/{id}/device-tokens")
    public ResponseEntity<List<DeviceToken>> getActiveTokens(@PathVariable Long id) {
        Optional<Customer> customer = customerRepository.findById(id);
        if (customer.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Long userId = customer.get().getUserId();
        if (userId == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(deviceTokenRepository.findByUserIdAndActiveTrue(userId));
    }

    /**
     * Task 6.5 — Register or update a device token for a user.
     * Upserts: if the token already exists, update deviceType and mark active; otherwise insert.
     * Expected body: { "userId": ..., "deviceToken": "...", "deviceType": "ANDROID|IOS|WEB" }
     */
    @PostMapping("/device-tokens/register")
    public ResponseEntity<DeviceToken> register(@RequestBody Map<String, Object> body) {
        String token = (String) body.get("deviceToken");
        String deviceType = (String) body.get("deviceType");
        Long userId = Long.valueOf(body.get("userId").toString());

        DeviceToken dt = deviceTokenRepository.findByDeviceToken(token)
                .orElse(new DeviceToken());
        dt.setUserId(userId);
        dt.setDeviceToken(token);
        dt.setDeviceType(deviceType);
        dt.setActive(true);
        dt.setLastSeenAt(Instant.now());
        return ResponseEntity.ok(deviceTokenRepository.save(dt));
    }

    /**
     * Task 6.6 — Deactivate a device token on logout.
     */
    @DeleteMapping("/device-tokens/{token}")
    public ResponseEntity<Void> deactivate(@PathVariable String token) {
        deviceTokenRepository.findByDeviceToken(token).ifPresent(dt -> {
            dt.setActive(false);
            deviceTokenRepository.save(dt);
        });
        return ResponseEntity.noContent().build();
    }
}
