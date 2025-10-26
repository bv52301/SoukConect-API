package com.souk.customer.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CustomerUpdateRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @Email String email,
        String phone
) {}
