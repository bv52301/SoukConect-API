package com.souk.vendor.api;

import com.souk.common.adapters.jpa.repository.DeviceTokenRepository;
import com.souk.common.adapters.jpa.repository.VendorRepository;
import com.souk.common.domain.DeviceToken;
import com.souk.common.domain.Vendor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * Task 6.4 — GET /vendors/{id}/device-tokens
 * Returns active device tokens for a vendor.
 * Used by OrderOrchestrationWorkflow to populate RecipientPreferences.deviceTokens for vendor notifications.
 */
@RestController
public class DeviceTokenController {

    private final VendorRepository vendorRepository;
    private final DeviceTokenRepository deviceTokenRepository;

    public DeviceTokenController(VendorRepository vendorRepository,
                                 DeviceTokenRepository deviceTokenRepository) {
        this.vendorRepository = vendorRepository;
        this.deviceTokenRepository = deviceTokenRepository;
    }

    @GetMapping("/vendors/{id}/device-tokens")
    public ResponseEntity<List<DeviceToken>> getActiveTokens(@PathVariable Long id) {
        Optional<Vendor> vendor = vendorRepository.findById(id);
        if (vendor.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Long userId = vendor.get().getUserId();
        if (userId == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(deviceTokenRepository.findByUserIdAndActiveTrue(userId));
    }
}
