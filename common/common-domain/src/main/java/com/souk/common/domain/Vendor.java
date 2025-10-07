package com.souk.common.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Entity
@Table(name = "vendor_details")
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vendor_id")
    private Long vendorId;

    @Column(nullable = false, length = 100)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "supportedCategories", columnDefinition = "json")
    private JsonNode supportedCategories;

    @Column(length = 300)
    private String image;

    @Column(length = 100)
    private String address1;

    @Column(length = 100)
    private String address2;

    @Column(length = 100)
    private String state;

    @Column(length = 255)
    private String landmark;

    @Column(length = 15)
    private String pincode;

    @Column(name = "contact_name", length = 100)
    private String contactName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(length = 100)
    private String email;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    // === Getters & Setters ===
    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public JsonNode getSupportedCategories() { return supportedCategories; }
    public void setSupportedCategories(JsonNode supportedCategories) { this.supportedCategories = supportedCategories; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getAddress1() { return address1; }
    public void setAddress1(String address1) { this.address1 = address1; }

    public String getAddress2() { return address2; }
    public void setAddress2(String address2) { this.address2 = address2; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getLandmark() { return landmark; }
    public void setLandmark(String landmark) { this.landmark = landmark; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
