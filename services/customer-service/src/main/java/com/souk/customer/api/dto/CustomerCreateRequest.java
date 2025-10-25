package com.souk.customer.api.dto;

import com.souk.common.domain.Customer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CustomerCreateRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @Email String email,
        String phone
) {
    public Customer toDomain() {
        Customer c = new Customer();
        c.setFirstName(firstName);
        c.setLastName(lastName);
        c.setEmail(email);
        c.setPhone(phone);
        return c;
    }
}
