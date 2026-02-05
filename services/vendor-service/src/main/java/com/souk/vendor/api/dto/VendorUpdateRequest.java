package com.souk.vendor.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.souk.common.domain.Address;
import com.souk.common.domain.Vendor;

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
) {
    public void updateDomain(Vendor v) {
        if (name != null) v.setName(name);
        if (description != null) v.setDescription(description);
        if (supportedCategories != null) v.setSupportedCategories(supportedCategories);
        if (schedule != null) v.setSchedule(schedule);
        if (image != null) v.setImage(image);
        if (contactName != null) v.setContactName(contactName);
        if (phoneNumber != null) v.setPhoneNumber(phoneNumber);
        if (email != null) v.setEmail(email);

        // Update or add primary address
        Address primary = v.getAddresses().stream()
                .filter(a -> "PRIMARY".equals(a.getAddressType()) || a.isDefault())
                .findFirst()
                .orElse(null);

        if (primary == null && (address1 != null || pincode != null)) {
            primary = new Address();
            primary.setAddressType("PRIMARY");
            primary.setDefault(true);
            v.addAddress(primary);
        }

        if (primary != null) {
            if (address1 != null) primary.setStreet(address1);
            if (address2 != null) primary.setUnit(address2);
            if (state != null) primary.setState(state);
            if (landmark != null) primary.setLandmark(landmark);
            if (pincode != null) primary.setPostalCode(pincode);
            if (latitude != null) primary.setLatitude(latitude);
            if (longitude != null) primary.setLongitude(longitude);
        }
    }
}
