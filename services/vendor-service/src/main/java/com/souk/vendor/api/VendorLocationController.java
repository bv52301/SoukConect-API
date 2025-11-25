package com.souk.vendor.api;

import com.souk.common.domain.VendorLocation;
import com.souk.common.port.DataAccessPort;
import com.souk.common.port.VendorLocationQueryPort;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * REST API for managing vendor locations (multiple physical locations per vendor)
 */
@RestController
@RequestMapping("/vendors/{vendorId}/locations")
public class VendorLocationController {

    private final DataAccessPort<VendorLocation, Long> locationPort;
    private final VendorLocationQueryPort locationQueryPort;

    public VendorLocationController(DataAccessPort<VendorLocation, Long> locationPort,
                                    VendorLocationQueryPort locationQueryPort) {
        this.locationPort = locationPort;
        this.locationQueryPort = locationQueryPort;
    }

    /**
     * Get all locations for a vendor
     */
    @GetMapping
    public List<VendorLocation> listLocations(@PathVariable @Min(1) Long vendorId,
                                               @RequestParam(required = false) String status) {
        if (status != null) {
            try {
                VendorLocation.Status statusEnum = VendorLocation.Status.valueOf(status.toUpperCase());
                return locationQueryPort.findByVendorIdAndStatus(vendorId, statusEnum);
            } catch (IllegalArgumentException e) {
                return locationQueryPort.findByVendorId(vendorId);
            }
        }
        return locationQueryPort.findByVendorId(vendorId);
    }

    /**
     * Get a specific location by ID
     */
    @GetMapping("/{locationId}")
    public ResponseEntity<VendorLocation> getLocation(@PathVariable @Min(1) Long vendorId,
                                                       @PathVariable @Min(1) Long locationId) {
        return locationPort.findById(locationId)
                .filter(loc -> loc.getVendorId().equals(vendorId))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new location for a vendor
     */
    @PostMapping
    public ResponseEntity<VendorLocation> createLocation(@PathVariable @Min(1) Long vendorId,
                                                          @RequestBody @Valid VendorLocation location) {
        location.setVendorId(vendorId);
        location.setId(null); // Ensure new entity
        VendorLocation saved = locationPort.save(location);
        return ResponseEntity
                .created(URI.create("/vendors/" + vendorId + "/locations/" + saved.getId()))
                .body(saved);
    }

    /**
     * Update an existing location
     */
    @PutMapping("/{locationId}")
    public ResponseEntity<VendorLocation> updateLocation(@PathVariable @Min(1) Long vendorId,
                                                          @PathVariable @Min(1) Long locationId,
                                                          @RequestBody @Valid VendorLocation location) {
        return locationPort.findById(locationId)
                .filter(existing -> existing.getVendorId().equals(vendorId))
                .map(existing -> {
                    location.setId(locationId);
                    location.setVendorId(vendorId);
                    VendorLocation saved = locationPort.save(location);
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
        return locationPort.findById(locationId)
                .filter(loc -> loc.getVendorId().equals(vendorId))
                .map(loc -> {
                    locationPort.deleteById(locationId);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
