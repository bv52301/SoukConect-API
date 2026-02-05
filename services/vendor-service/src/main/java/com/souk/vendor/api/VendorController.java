package com.souk.vendor.api;

import com.souk.common.domain.Vendor;
import com.souk.common.domain.Address;
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
    private final DataAccessPort<Address, Long> addressPort;

    public VendorController(DataAccessPort<Vendor, Long> vendorPort,
                           VendorRepository vendorRepo,
                           DataAccessPort<Address, Long> addressPort) {
        this.vendorPort = vendorPort;
        this.vendorRepo = vendorRepo;
        this.addressPort = addressPort;
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

    // --- Find vendors by supported category (includes product search) ---
    @GetMapping("/by-category")
    public List<VendorResponse> listByCategory(@RequestParam("category") String category) {
        if (category == null || category.isBlank()) return List.of();
        return vendorRepo.findByCategoryIncludingProducts(category.trim()).stream()
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
                    req.updateDomain(existing);
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
        return addressPort.findAll().stream().filter(a -> a.getOwnerType() == Address.OwnerType.VENDOR)
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

                    // Update basic fields if provided
                    if (row.name != null && !row.name.isEmpty()) vendor.setName(row.name);
                    if (row.email != null && !row.email.isEmpty()) vendor.setEmail(row.email);
                    if (row.phoneNumber != null && !row.phoneNumber.isEmpty()) vendor.setPhoneNumber(row.phoneNumber);
                    if (row.description != null) vendor.setDescription(row.description);
                    if (row.contactName != null) vendor.setContactName(row.contactName);
                    if (row.supportedCategories != null) vendor.setSupportedCategories(row.supportedCategories);
                    if (row.schedule != null) vendor.setSchedule(row.schedule);
                    if (row.image != null) vendor.setImage(row.image);

                    // Update or Add Primary Address
                    Address primary = vendor.getAddresses().stream()
                            .filter(a -> "PRIMARY".equals(a.getAddressType()) || a.isDefault())
                            .findFirst()
                            .orElse(null);

                    if (primary == null && (row.address1 != null || row.pincode != null)) {
                        primary = new Address();
                        primary.setAddressType("PRIMARY");
                        primary.setDefault(true);
                        vendor.addAddress(primary);
                    }

                    if (primary != null) {
                        if (row.address1 != null) primary.setStreet(row.address1);
                        if (row.address2 != null) primary.setUnit(row.address2);
                        if (row.state != null) primary.setState(row.state);
                        if (row.landmark != null) primary.setLandmark(row.landmark);
                        if (row.pincode != null) primary.setPostalCode(row.pincode);
                        if (row.latitude != null) primary.setLatitude(row.latitude);
                        if (row.longitude != null) primary.setLongitude(row.longitude);
                        
                        // Default coords if missing
                        if (primary.getLatitude() == null) primary.setLatitude(BigDecimal.ZERO);
                        if (primary.getLongitude() == null) primary.setLongitude(BigDecimal.ZERO);
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
