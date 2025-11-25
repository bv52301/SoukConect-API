package com.souk.vendor.api;

import com.souk.common.domain.Vendor;
import com.souk.common.domain.VendorLocation;
import com.souk.common.port.DataAccessPort;
import com.souk.common.adapters.jpa.repository.VendorRepository;
import com.souk.vendor.util.VendorExcelParser;
import com.souk.vendor.api.dto.VendorBulkUploadResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.souk.vendor.api.dto.VendorCreateRequest;
import com.souk.vendor.api.dto.VendorUpdateRequest;
import com.souk.vendor.api.dto.VendorResponse;
import com.souk.vendor.api.dto.VendorLocationResponse;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
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

    // --- Bulk upload vendors from Excel file ---
    @PostMapping(value = "/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VendorBulkUploadResult> bulkUpload(
            @RequestPart("file") MultipartFile file
    ) {
        try {
            List<VendorExcelParser.VendorRow> rows = VendorExcelParser.parseExcel(file);

            List<VendorBulkUploadResult.RowResult> results = new ArrayList<>();
            int successCount = 0;
            int failureCount = 0;

            for (int i = 0; i < rows.size(); i++) {
                VendorExcelParser.VendorRow row = rows.get(i);
                int rowNumber = i + 2; // Excel row (1-indexed + 1 for header)

                try {
                    // Try to find existing vendor by email or phone
                    Optional<Vendor> existingVendor = Optional.empty();
                    if (row.email != null && !row.email.isEmpty()) {
                        existingVendor = vendorPort.findAll().stream()
                                .filter(v -> row.email.equalsIgnoreCase(v.getEmail()))
                                .findFirst();
                    } else if (row.phoneNumber != null && !row.phoneNumber.isEmpty()) {
                        existingVendor = vendorPort.findAll().stream()
                                .filter(v -> row.phoneNumber.equals(v.getPhoneNumber()))
                                .findFirst();
                    }

                    Vendor vendor;
                    boolean isUpdate = existingVendor.isPresent();

                    if (isUpdate) {
                        vendor = existingVendor.get();
                    } else {
                        vendor = new Vendor();
                    }

                    // Update fields if provided
                    if (row.name != null && !row.name.isEmpty()) {
                        vendor.setName(row.name);
                    }
                    if (row.email != null && !row.email.isEmpty()) {
                        vendor.setEmail(row.email);
                    }
                    if (row.phoneNumber != null && !row.phoneNumber.isEmpty()) {
                        vendor.setPhoneNumber(row.phoneNumber);
                    }
                    if (row.description != null) {
                        vendor.setDescription(row.description);
                    }
                    if (row.address1 != null) {
                        vendor.setAddress1(row.address1);
                    }
                    if (row.address2 != null) {
                        vendor.setAddress2(row.address2);
                    }
                    if (row.state != null) {
                        vendor.setState(row.state);
                    }
                    if (row.pincode != null) {
                        vendor.setPincode(row.pincode);
                    }
                    if (row.landmark != null) {
                        vendor.setLandmark(row.landmark);
                    }
                    if (row.contactName != null) {
                        vendor.setContactName(row.contactName);
                    }
                    if (row.latitude != null) {
                        vendor.setLatitude(row.latitude);
                    }
                    if (row.longitude != null) {
                        vendor.setLongitude(row.longitude);
                    }
                    if (row.supportedCategories != null) {
                        vendor.setSupportedCategories(row.supportedCategories);
                    }
                    if (row.schedule != null) {
                        vendor.setSchedule(row.schedule);
                    }
                    if (row.image != null) {
                        vendor.setImage(row.image);
                    }

                    // Ensure latitude and longitude have defaults if not set
                    if (vendor.getLatitude() == null) {
                        vendor.setLatitude(BigDecimal.ZERO);
                    }
                    if (vendor.getLongitude() == null) {
                        vendor.setLongitude(BigDecimal.ZERO);
                    }

                    // Save the vendor
                    Vendor saved = vendorPort.save(vendor);

                    String message = isUpdate ? "Updated successfully" : "Created successfully";
                    results.add(new VendorBulkUploadResult.RowResult(
                            rowNumber, row.name, true, message, saved.getVendorId()
                    ));
                    successCount++;

                } catch (Exception e) {
                    results.add(new VendorBulkUploadResult.RowResult(
                            rowNumber, row.name, false, "Error: " + e.getMessage(), null
                    ));
                    failureCount++;
                }
            }

            VendorBulkUploadResult result = new VendorBulkUploadResult(
                    rows.size(), successCount, failureCount, results
            );

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new VendorBulkUploadResult(
                            0, 0, 0,
                            List.of(new VendorBulkUploadResult.RowResult(
                                    0, "", false, "Failed to parse Excel file: " + e.getMessage(), null
                            ))
                    )
            );
        }
    }
}
