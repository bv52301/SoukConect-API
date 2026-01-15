package com.souk.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MfaVerifyRequest {

    @NotBlank(message = "MFA code is required")
    @Size(min = 6, max = 6, message = "MFA code must be 6 digits")
    private String code;

    private String mfaToken;

    public MfaVerifyRequest() {}

    public MfaVerifyRequest(String code, String mfaToken) {
        this.code = code;
        this.mfaToken = mfaToken;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getMfaToken() { return mfaToken; }
    public void setMfaToken(String mfaToken) { this.mfaToken = mfaToken; }
}
