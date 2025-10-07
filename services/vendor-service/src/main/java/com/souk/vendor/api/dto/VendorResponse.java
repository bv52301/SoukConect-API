package com.souk.vendor.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.souk.common.domain.Vendor;

import java.time.LocalDateTime;

public record VendorResponse(
        Long vendorId,
        String name,
        JsonNode supportedCategories,
        String image,
        String address1,
        String address2,
        String state,
        String landmark,
        String pincode,
        String contactName,
        String phoneNumber,
        String email,
        LocalDateTime createdAt
) {
    public static VendorResponse from(Vendor v) {
        return new VendorResponse(
                v.getVendorId(),
                v.getName(),
                v.getSupportedCategories(),
                v.getImage(),
                v.getAddress1(),
                v.getAddress2(),
                v.getState(),
                v.getLandmark(),
                v.getPincode(),
                v.getContactName(),
                v.getPhoneNumber(),
                v.getEmail(),
                v.getCreatedAt()
        );
    }
}
