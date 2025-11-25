package com.souk.vendor.api;

import com.souk.common.domain.Vendor;
import com.souk.common.domain.VendorLocation;
import com.souk.common.port.DataAccessPort;
import com.souk.common.adapters.jpa.repository.VendorRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.souk.vendor.api.dto.VendorCreateRequest;
import com.souk.vendor.api.dto.VendorUpdateRequest;
import com.souk.vendor.api.dto.VendorResponse;
import com.souk.vendor.api.dto.VendorLocationResponse;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/vendors")
public class VendorController {

    private final DataAccessPort<Vendor, Long> vendorPort;
    private final VendorRepository vendorRepo;
    private final DataAccessPort<VendorLocation, Long> vendorLocationPort;

    public VendorController(DataAccessPort<Vendor, Long> vendorPort,
                           VendorRepository vendorRepo,
                           DataAccessPort<VendorLocation, Long> vendorLocationPort) {
        this.vendorPort = vendorPort;
        this.vendorRepo = vendorRepo;
        this.vendorLocationPort = vendorLocationPort;
    }

    // --- List all vendors ---
    @GetMapping
    public List<VendorResponse> listAll(@RequestParam(value = "q", required = false) String q) {
        var stream = vendorPort.findAll().stream();
        if (q != null && !q.isBlank()) {
            final String needle = q.toLowerCase();
            stream = stream.filter(v ->
                    (v.getName() != null && v.getName().toLowerCase().contains(needle)) ||
                    (v.getEmail() != null && v.getEmail().toLowerCase().contains(needle)) ||
                    (v.getPhoneNumber() != null && v.getPhoneNumber().toLowerCase().contains(needle)) ||
                    (v.getVendorId() != null && String.valueOf(v.getVendorId()).contains(needle))
            );
        }
        return stream.map(VendorResponse::from).toList();
    }

    // --- Find vendors by supported category ---
    @GetMapping("/by-category")
    public List<VendorResponse> listByCategory(@RequestParam("category") String category) {
        if (category == null || category.isBlank()) return List.of();
        return vendorRepo.findByCategory(category.trim()).stream()
                .map(VendorResponse::from)
                .toList();
    }

    // --- Get vendor by ID ---
    @GetMapping("/{id}")
    public ResponseEntity<VendorResponse> getById(@PathVariable @Min(1) Long id) {
        return vendorPort.findById(id)
                .map(VendorResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // --- Create vendor ---
    @PostMapping
    public ResponseEntity<VendorResponse> create(@RequestBody @Valid VendorCreateRequest req) {
        Vendor toSave = req.toDomain();
        Vendor saved = vendorPort.save(toSave);
        return ResponseEntity
                .created(URI.create("/vendors/" + saved.getVendorId()))
                .body(VendorResponse.from(saved));
    }

    // --- Update vendor ---
    @PutMapping("/{id}")
    public ResponseEntity<VendorResponse> update(@PathVariable @Min(1) Long id,
                                                 @RequestBody @Valid VendorUpdateRequest req) {
        return vendorPort.findById(id)
                .map(existing -> {
                    existing.setName(req.name());
                    existing.setDescription(req.description());
                    existing.setSupportedCategories(req.supportedCategories());
                    existing.setSchedule(req.schedule());
                    existing.setImage(req.image());
                    existing.setAddress1(req.address1());
                    existing.setAddress2(req.address2());
                    existing.setState(req.state());
                    existing.setLandmark(req.landmark());
                    existing.setPincode(req.pincode());
                    existing.setContactName(req.contactName());
                    existing.setPhoneNumber(req.phoneNumber());
                    existing.setEmail(req.email());
                    if (req.latitude() != null) existing.setLatitude(req.latitude());
                    if (req.longitude() != null) existing.setLongitude(req.longitude());
                    Vendor saved = vendorPort.save(existing);
                    return ResponseEntity.ok(VendorResponse.from(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // --- Delete vendor ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Min(1) Long id) {
        return vendorPort.findById(id)
                .map(v -> {
                    vendorPort.deleteById(id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().<Void>build());
    }

    // --- Get all vendor locations (for multi-select in product form) ---
    @GetMapping("/locations")
    public List<VendorLocationResponse> getAllVendorLocations() {
        return vendorLocationPort.findAll().stream()
                .map(VendorLocationResponse::from)
                .toList();
    }
}
