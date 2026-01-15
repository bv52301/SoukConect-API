package com.souk.auth.api.dto;

import java.util.Set;

public class TokenResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private long expiresIn;
    private UserInfo user;
    private boolean mfaRequired;
    private String mfaToken;

    public TokenResponse() {}

    public static TokenResponse success(String accessToken, String refreshToken, long expiresIn, UserInfo user) {
        TokenResponse response = new TokenResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(expiresIn);
        response.setUser(user);
        response.setMfaRequired(false);
        return response;
    }

    public static TokenResponse mfaRequired(String mfaToken) {
        TokenResponse response = new TokenResponse();
        response.setMfaRequired(true);
        response.setMfaToken(mfaToken);
        return response;
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }

    public UserInfo getUser() { return user; }
    public void setUser(UserInfo user) { this.user = user; }

    public boolean isMfaRequired() { return mfaRequired; }
    public void setMfaRequired(boolean mfaRequired) { this.mfaRequired = mfaRequired; }

    public String getMfaToken() { return mfaToken; }
    public void setMfaToken(String mfaToken) { this.mfaToken = mfaToken; }

    public static class UserInfo {
        private Long userId;
        private String email;
        private Set<String> roles;
        private boolean emailVerified;
        private boolean mfaEnabled;
        private String profilePictureUrl;

        public UserInfo() {}

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public Set<String> getRoles() { return roles; }
        public void setRoles(Set<String> roles) { this.roles = roles; }

        public boolean isEmailVerified() { return emailVerified; }
        public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

        public boolean isMfaEnabled() { return mfaEnabled; }
        public void setMfaEnabled(boolean mfaEnabled) { this.mfaEnabled = mfaEnabled; }

        public String getProfilePictureUrl() { return profilePictureUrl; }
        public void setProfilePictureUrl(String profilePictureUrl) { this.profilePictureUrl = profilePictureUrl; }
    }
}
