package com.souk.auth.api.dto;

public class MfaSetupResponse {

    private String secret;
    private String qrCodeUrl;
    private String manualEntryKey;

    public MfaSetupResponse() {}

    public MfaSetupResponse(String secret, String qrCodeUrl, String manualEntryKey) {
        this.secret = secret;
        this.qrCodeUrl = qrCodeUrl;
        this.manualEntryKey = manualEntryKey;
    }

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public String getQrCodeUrl() { return qrCodeUrl; }
    public void setQrCodeUrl(String qrCodeUrl) { this.qrCodeUrl = qrCodeUrl; }

    public String getManualEntryKey() { return manualEntryKey; }
    public void setManualEntryKey(String manualEntryKey) { this.manualEntryKey = manualEntryKey; }
}
