package com.souk.common.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "user_roles")
@IdClass(UserRole.UserRoleId.class)
public class UserRole {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    public enum Role {
        CUSTOMER,
        VENDOR,
        ADMIN,
        SUPPORT
    }

    // Composite key class
    public static class UserRoleId implements Serializable {
        private Long user;
        private Role role;

        public UserRoleId() {}

        public UserRoleId(Long user, Role role) {
            this.user = user;
            this.role = role;
        }

        public Long getUser() { return user; }
        public void setUser(Long user) { this.user = user; }

        public Role getRole() { return role; }
        public void setRole(Role role) { this.role = role; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UserRoleId that = (UserRoleId) o;
            return Objects.equals(user, that.user) && role == that.role;
        }

        @Override
        public int hashCode() {
            return Objects.hash(user, role);
        }
    }

    // === Getters & Setters ===
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserRole userRole = (UserRole) o;
        return Objects.equals(user, userRole.user) && role == userRole.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, role);
    }
}
