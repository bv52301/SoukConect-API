package com.souk.vendor.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.souk.common.domain.Vendor;

import java.math.BigDecimal;

public record VendorCreateRequest(
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
) {
    public Vendor toDomain() {
        Vendor v = new Vendor();
        v.setName(name);
        v.setDescription(description);
        v.setSupportedCategories(supportedCategories);
        v.setSchedule(schedule);
        v.setImage(image);
        v.setAddress1(address1);
        v.setAddress2(address2);
        v.setState(state);
        v.setLandmark(landmark);
        v.setPincode(pincode);
        v.setContactName(contactName);
        v.setPhoneNumber(phoneNumber);
        v.setEmail(email);
        v.setLatitude(latitude != null ? latitude : BigDecimal.ZERO);
        v.setLongitude(longitude != null ? longitude : BigDecimal.ZERO);
        return v;
    }
}
