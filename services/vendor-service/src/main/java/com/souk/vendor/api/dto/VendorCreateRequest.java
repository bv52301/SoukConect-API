package com.souk.vendor.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.souk.common.domain.Vendor;

public record VendorCreateRequest(
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
) {
    public Vendor toDomain() {
        Vendor v = new Vendor();
        v.setName(name);
        v.setSupportedCategories(supportedCategories);
        v.setImage(image);
        v.setAddress1(address1);
        v.setAddress2(address2);
        v.setState(state);
        v.setLandmark(landmark);
        v.setPincode(pincode);
        v.setContactName(contactName);
        v.setPhoneNumber(phoneNumber);
        v.setEmail(email);
        return v;
    }
}
