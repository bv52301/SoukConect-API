package com.souk.payment.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for payment gateways.
 * Loaded from application.yml under 'payment.gateway' prefix.
 * Bean is registered via @EnableConfigurationProperties in GatewayConfig.
 */
@ConfigurationProperties(prefix = "payment.gateway")
public class GatewayProperties {

    private String defaultGateway = "STRIPE";
    private StripeProperties stripe = new StripeProperties();
    private CMIProperties cmi = new CMIProperties();

    // === Getters & Setters ===
    public String getDefaultGateway() { return defaultGateway; }
    public void setDefaultGateway(String defaultGateway) { this.defaultGateway = defaultGateway; }

    public StripeProperties getStripe() { return stripe; }
    public void setStripe(StripeProperties stripe) { this.stripe = stripe; }

    public CMIProperties getCmi() { return cmi; }
    public void setCmi(CMIProperties cmi) { this.cmi = cmi; }

    // === Stripe Configuration ===
    public static class StripeProperties {
        private boolean enabled = false;
        private String secretKey;
        private String publishableKey;
        private String webhookSecret;
        private String apiVersion = "2023-10-16";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

        public String getPublishableKey() { return publishableKey; }
        public void setPublishableKey(String publishableKey) { this.publishableKey = publishableKey; }

        public String getWebhookSecret() { return webhookSecret; }
        public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }

        public String getApiVersion() { return apiVersion; }
        public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }
    }

    // === CMI Configuration ===
    public static class CMIProperties {
        private boolean enabled = false;
        private String apiUrl;
        private String merchantId;
        private String terminalId;
        private String secretKey;
        private String storeKey;
        private boolean testMode = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getApiUrl() { return apiUrl; }
        public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }

        public String getMerchantId() { return merchantId; }
        public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

        public String getTerminalId() { return terminalId; }
        public void setTerminalId(String terminalId) { this.terminalId = terminalId; }

        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

        public String getStoreKey() { return storeKey; }
        public void setStoreKey(String storeKey) { this.storeKey = storeKey; }

        public boolean isTestMode() { return testMode; }
        public void setTestMode(boolean testMode) { this.testMode = testMode; }
    }
}
