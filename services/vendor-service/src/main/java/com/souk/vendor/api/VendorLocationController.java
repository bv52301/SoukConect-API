package com.souk.vendor.api;

import com.souk.common.domain.Address;
import com.souk.common.port.AddressQueryPort;
import com.souk.common.port.DataAccessPort;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * REST API for managing vendor locations (multiple physical locations per vendor)
 * Now uses the unified Address entity.
 */
@RestController
@RequestMapping("/vendors/{vendorId}/locations")
public class VendorLocationController {

    private final DataAccessPort<Address, Long> addressPort;
    private final AddressQueryPort addressQueryPort;

    public VendorLocationController(DataAccessPort<Address, Long> addressPort,
                                    AddressQueryPort addressQueryPort) {
        this.addressPort = addressPort;
        this.addressQueryPort = addressQueryPort;
    }

    /**
     * Get all locations for a vendor
     */
    @GetMapping
    public List<Address> listLocations(@PathVariable @Min(1) Long vendorId,
                                        @RequestParam(required = false) Boolean active) {
        List<Address> addresses = addressQueryPort.findByOwnerIdAndOwnerType(vendorId, Address.OwnerType.VENDOR);
        if (active != null) {
            return addresses.stream()
                    .filter(a -> a.isActive() == active)
                    .toList();
        }
        return addresses;
    }

    /**
     * Get a specific location by ID
     */
    @GetMapping("/{locationId}")
    public ResponseEntity<Address> getLocation(@PathVariable @Min(1) Long vendorId,
                                                @PathVariable @Min(1) Long locationId) {
        return addressPort.findById(locationId)
                .filter(addr -> addr.getOwnerId().equals(vendorId) && addr.getOwnerType() == Address.OwnerType.VENDOR)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new location for a vendor
     */
    @PostMapping
    public ResponseEntity<Address> createLocation(@PathVariable @Min(1) Long vendorId,
                                                   @RequestBody @Valid Address location) {
        location.setOwnerId(vendorId);
        location.setOwnerType(Address.OwnerType.VENDOR);
        location.setId(null); // Ensure new entity
        if (location.getAddressType() == null) {
            location.setAddressType("BRANCH");
        }
        Address saved = addressPort.save(location);
        return ResponseEntity
                .created(URI.create("/vendors/" + vendorId + "/locations/" + saved.getId()))
                .body(saved);
    }

    /**
     * Update an existing location
     */
    @PutMapping("/{locationId}")
    public ResponseEntity<Address> updateLocation(@PathVariable @Min(1) Long vendorId,
                                                   @PathVariable @Min(1) Long locationId,
                                                   @RequestBody @Valid Address location) {
        return addressPort.findById(locationId)
                .filter(existing -> existing.getOwnerId().equals(vendorId) && existing.getOwnerType() == Address.OwnerType.VENDOR)
                .map(existing -> {
                    location.setId(locationId);
                    location.setOwnerId(vendorId);
                    location.setOwnerType(Address.OwnerType.VENDOR);
                    if (location.getAddressType() == null) {
                        location.setAddressType("BRANCH");
                    }
                    Address saved = addressPort.save(location);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete a location
     */
    @DeleteMapping("/{locationId}")
    public ResponseEntity<Void> deleteLocation(@PathVariable @Min(1) Long vendorId,
                                                @PathVariable @Min(1) Long locationId) {
        return addressPort.findById(locationId)
                .filter(addr -> addr.getOwnerId().equals(vendorId) && addr.getOwnerType() == Address.OwnerType.VENDOR)
                .map(addr -> {
                    addressPort.deleteById(locationId);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
