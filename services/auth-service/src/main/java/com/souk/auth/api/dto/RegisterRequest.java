package com.souk.auth.api.dto;

import com.souk.common.domain.UserRole.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @Size(max = 30, message = "Phone number cannot exceed 30 characters")
    private String phone;

    @NotNull(message = "Role is required")
    private Role role;

    // Customer fields (required when role=CUSTOMER)
    private String firstName;
    private String lastName;

    // Customer address fields (optional)
    private String street;
    private String unit;
    private String city;
    private String postal;
    private String country;

    // Vendor fields (required when role=VENDOR)
    private String businessName;
    private String businessDescription;
    private String contactName;
    private String address1;
    private String address2;
    private String state;
    private String landmark;
    private String pincode;
    private BigDecimal latitude;
    private BigDecimal longitude;

    public RegisterRequest() {}

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    // Customer address getters and setters
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPostal() { return postal; }
    public void setPostal(String postal) { this.postal = postal; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    // Vendor getters and setters
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public String getBusinessDescription() { return businessDescription; }
    public void setBusinessDescription(String businessDescription) { this.businessDescription = businessDescription; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

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

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
}
