package com.souk.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.email")
public class EmailProperties {

    private String fromAddress = "noreply@soukconect.com";
    private String fromName = "SoukConect";
    private String baseUrl = "http://localhost:3000";
    private String verifyEmailPath = "/verify-email";
    private String resetPasswordPath = "/reset-password";

    public String getFromAddress() { return fromAddress; }
    public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }

    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getVerifyEmailPath() { return verifyEmailPath; }
    public void setVerifyEmailPath(String verifyEmailPath) { this.verifyEmailPath = verifyEmailPath; }

    public String getResetPasswordPath() { return resetPasswordPath; }
    public void setResetPasswordPath(String resetPasswordPath) { this.resetPasswordPath = resetPasswordPath; }

    public String getVerifyEmailUrl(String token) {
        return baseUrl + verifyEmailPath + "?token=" + token;
    }

    public String getResetPasswordUrl(String token) {
        return baseUrl + resetPasswordPath + "?token=" + token;
    }
}
