package com.souk.common.adapters.jpa.repository;

import com.souk.common.domain.User;
import com.souk.common.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRole.UserRoleId> {

    List<UserRole> findByUser(User user);

    List<UserRole> findByUser_UserId(Long userId);

    Optional<UserRole> findByUser_UserIdAndRole(Long userId, UserRole.Role role);

    boolean existsByUser_UserIdAndRole(Long userId, UserRole.Role role);

    void deleteByUser_UserIdAndRole(Long userId, UserRole.Role role);
}
