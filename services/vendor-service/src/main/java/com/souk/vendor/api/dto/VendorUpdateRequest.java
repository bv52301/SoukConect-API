package com.souk.vendor.api.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record VendorUpdateRequest(
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
        String email
) {}
