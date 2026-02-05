package com.souk.common.adapters.jpa.repository;

import com.souk.common.domain.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserId(Long userId);
    List<Address> findByOwnerIdAndOwnerType(Long ownerId, Address.OwnerType ownerType);
}
