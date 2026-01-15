package com.souk.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.oauth")
public class OAuthProperties {

    private Google google = new Google();
    private Apple apple = new Apple();
    private Facebook facebook = new Facebook();
    private String successRedirectUrl = "http://localhost:3000/oauth/callback";
    private String failureRedirectUrl = "http://localhost:3000/login?error=oauth_failed";

    public Google getGoogle() { return google; }
    public void setGoogle(Google google) { this.google = google; }

    public Apple getApple() { return apple; }
    public void setApple(Apple apple) { this.apple = apple; }

    public Facebook getFacebook() { return facebook; }
    public void setFacebook(Facebook facebook) { this.facebook = facebook; }

    public String getSuccessRedirectUrl() { return successRedirectUrl; }
    public void setSuccessRedirectUrl(String successRedirectUrl) { this.successRedirectUrl = successRedirectUrl; }

    public String getFailureRedirectUrl() { return failureRedirectUrl; }
    public void setFailureRedirectUrl(String failureRedirectUrl) { this.failureRedirectUrl = failureRedirectUrl; }

    public static class Google {
        private String clientId;
        private String clientSecret;
        private boolean enabled = false;

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }

        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class Apple {
        private String clientId;
        private String teamId;
        private String keyId;
        private String privateKey;
        private boolean enabled = false;

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }

        public String getTeamId() { return teamId; }
        public void setTeamId(String teamId) { this.teamId = teamId; }

        public String getKeyId() { return keyId; }
        public void setKeyId(String keyId) { this.keyId = keyId; }

        public String getPrivateKey() { return privateKey; }
        public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class Facebook {
        private String clientId;
        private String clientSecret;
        private boolean enabled = false;

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }

        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
