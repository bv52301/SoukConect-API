package com.souk.vendor.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.souk.common.domain.VendorLocation;

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
        String status
) {
    public static VendorLocationResponse from(VendorLocation location) {
        return new VendorLocationResponse(
                location.getId(),
                location.getVendorId(),
                location.getLocationName(),
                location.getLatitude(),
                location.getLongitude(),
                location.getAddress1(),
                location.getAddress2(),
                location.getState(),
                location.getPincode(),
                location.getLandmark(),
                location.getStatus() != null ? location.getStatus().name() : null
        );
    }
}
