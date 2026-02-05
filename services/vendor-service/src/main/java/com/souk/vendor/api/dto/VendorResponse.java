package com.souk.vendor.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.souk.common.domain.Address;
import com.souk.common.domain.Vendor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VendorResponse(
        Long vendorId,
        String name,
        String description,
        JsonNode supportedCategories,
        JsonNode schedule,
        String image,
        String address1,
        String address2,
        String state,
        String landmark,
        String pincode,
        String contactName,
        String phoneNumber,
        String email,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalDateTime createdAt,
        LocalDateTime scheduleUpdated,
        List<AddressResponse> addresses
) {
    public static VendorResponse from(Vendor v) {
        // Try to find primary address for flat fields (backward compatibility)
        Address primary = v.getAddresses().stream()
                .filter(a -> "PRIMARY".equals(a.getAddressType()) || a.isDefault())
                .findFirst()
                .orElse(null);

        return new VendorResponse(
                v.getVendorId(),
                v.getName(),
                v.getDescription(),
                v.getSupportedCategories(),
                v.getSchedule(),
                v.getImage(),
                primary != null ? primary.getStreet() : null,
                primary != null ? primary.getUnit() : null,
                primary != null ? primary.getState() : null,
                primary != null ? primary.getLandmark() : null,
                primary != null ? primary.getPostalCode() : null,
                v.getContactName(),
                v.getPhoneNumber(),
                v.getEmail(),
                primary != null ? primary.getLatitude() : null,
                primary != null ? primary.getLongitude() : null,
                v.getCreatedAt(),
                v.getScheduleUpdated(),
                v.getAddresses().stream().map(AddressResponse::from).toList()
        );
    }

    public record AddressResponse(
            Long id,
            String label,
            String addressType,
            String street,
            String unit,
            String city,
            String state,
            String postalCode,
            String country,
            String landmark,
            BigDecimal latitude,
            BigDecimal longitude,
            boolean isDefault
    ) {
        public static AddressResponse from(Address a) {
            return new AddressResponse(
                    a.getId(),
                    a.getLabel(),
                    a.getAddressType(),
                    a.getStreet(),
                    a.getUnit(),
                    a.getCity(),
                    a.getState(),
                    a.getPostalCode(),
                    a.getCountry(),
                    a.getLandmark(),
                    a.getLatitude(),
                    a.getLongitude(),
                    a.isDefault()
            );
        }
    }
}
