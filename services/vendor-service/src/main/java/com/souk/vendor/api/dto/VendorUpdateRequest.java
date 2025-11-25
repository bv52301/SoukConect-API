package com.souk.vendor.api.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

public record VendorUpdateRequest(
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
        BigDecimal longitude
) {}
