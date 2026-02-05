package com.souk.common.port;

import com.souk.common.domain.Address;
import java.util.List;

/**
 * Domain-specific query port for Address lookups
 */
public interface AddressQueryPort {
    List<Address> findByUserId(Long userId);
    List<Address> findByOwnerIdAndOwnerType(Long ownerId, Address.OwnerType ownerType);
}
