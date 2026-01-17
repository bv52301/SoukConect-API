package com.souk.combined.security;

import java.io.Serializable;
import java.util.Set;

public class UserPrincipal implements Serializable {

    private final Long userId;
    private final String email;
    private final Set<String> roles;

    public UserPrincipal(Long userId, String email, Set<String> roles) {
        this.userId = userId;
        this.email = email;
        this.roles = roles;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    public boolean isVendor() {
        return hasRole("VENDOR");
    }

    public boolean isCustomer() {
        return hasRole("CUSTOMER");
    }

    @Override
    public String toString() {
        return "UserPrincipal{userId=" + userId + ", email='" + email + "', roles=" + roles + "}";
    }
}
