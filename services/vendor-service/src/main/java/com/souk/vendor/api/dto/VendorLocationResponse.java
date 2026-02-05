package com.souk.vendor.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.souk.common.domain.Address;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VendorLocationResponse(
        Long id,
        Long vendorId,
        String locationName,
        BigDecimal latitude,
        BigDecimal longitude,
        String address1,
        String address2,
        String state,
        String pincode,
        String landmark,
        String status,
        String addressType,
        Boolean isDefault
) {
    public static VendorLocationResponse from(Address address) {
        return new VendorLocationResponse(
                address.getId(),
                address.getOwnerId(),
                address.getLabel(), // Using label as locationName
                address.getLatitude(),
                address.getLongitude(),
                address.getStreet(),
                address.getUnit(),
                address.getState(),
                address.getPostalCode(),
                address.getLandmark(),
                address.isActive() ? "ACTIVE" : "INACTIVE",
                address.getAddressType(),
                address.isDefault()
        );
    }
}
