package com.souk.vendor.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.souk.common.domain.Address;
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
        v.setContactName(contactName);
        v.setPhoneNumber(phoneNumber);
        v.setEmail(email);

        // Map primary address fields to a new Address entity
        if (address1 != null || pincode != null || latitude != null || longitude != null) {
            Address addr = new Address();
            addr.setStreet(address1);
            addr.setUnit(address2);
            addr.setState(state);
            addr.setLandmark(landmark);
            addr.setPostalCode(pincode);
            addr.setLatitude(latitude != null ? latitude : BigDecimal.ZERO);
            addr.setLongitude(longitude != null ? longitude : BigDecimal.ZERO);
            addr.setAddressType("PRIMARY");
            addr.setDefault(true);
            v.addAddress(addr);
        }

        return v;
    }
}
