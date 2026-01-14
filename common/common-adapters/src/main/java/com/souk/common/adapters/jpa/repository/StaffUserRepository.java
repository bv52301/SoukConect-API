package com.souk.common.adapters.jpa.repository;

import com.souk.common.domain.StaffUser;
import com.souk.common.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffUserRepository extends JpaRepository<StaffUser, Long> {

    Optional<StaffUser> findByUser(User user);

    Optional<StaffUser> findByUser_UserId(Long userId);

    Optional<StaffUser> findByEmployeeId(String employeeId);

    List<StaffUser> findByDepartment(String department);

    List<StaffUser> findByIsActive(Boolean isActive);

    boolean existsByEmployeeId(String employeeId);
}
