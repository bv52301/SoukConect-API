package com.souk.common.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;


@Entity
@Table(name = "vendor_details")
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vendor_id")
    private Long vendorId;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 1000)
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "supportedCategories", columnDefinition = "json")
    private JsonNode supportedCategories;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "schedule", columnDefinition = "json")
    private JsonNode schedule;

    @Column(length = 300)
    private String image;

    @Column(name = "contact_name", length = 100)
    private String contactName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(length = 100)
    private String email;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "schedule_updated", insertable = false, updatable = false)
    private LocalDateTime scheduleUpdated;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", referencedColumnName = "vendor_id")
    @Where(clause = "owner_type = 'VENDOR'")
    private List<Address> addresses = new ArrayList<>();

    // === Getters & Setters ===
    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public JsonNode getSupportedCategories() { return supportedCategories; }
    public void setSupportedCategories(JsonNode supportedCategories) { this.supportedCategories = supportedCategories; }

    public JsonNode getSchedule() { return schedule; }
    public void setSchedule(JsonNode schedule) { this.schedule = schedule; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getScheduleUpdated() { return scheduleUpdated; }

    public List<Address> getAddresses() { return addresses; }
    public void setAddresses(List<Address> addresses) {
        this.addresses.clear();
        if (addresses != null) {
            addresses.forEach(a -> {
                a.setOwnerId(this.vendorId);
                a.setOwnerType(Address.OwnerType.VENDOR);
                if (this.userId != null) a.setUserId(this.userId);
            });
            this.addresses.addAll(addresses);
        }
    }

    public void addAddress(Address address) {
        address.setOwnerId(this.vendorId);
        address.setOwnerType(Address.OwnerType.VENDOR);
        if (this.userId != null) address.setUserId(this.userId);
        this.addresses.add(address);
    }
}
