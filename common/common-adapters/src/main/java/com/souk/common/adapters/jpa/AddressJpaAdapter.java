package com.souk.common.adapters.jpa;

import com.souk.common.adapters.jpa.repository.AddressRepository;
import com.souk.common.domain.Address;
import com.souk.common.port.AddressQueryPort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AddressJpaAdapter extends JpaDataAccessAdapter<Address, Long>
        implements AddressQueryPort {

    private final AddressRepository repo;

    public AddressJpaAdapter(AddressRepository repo) {
        super(repo);
        this.repo = repo;
    }

    @Override
    public List<Address> findByUserId(Long userId) {
        return repo.findByUserId(userId);
    }

    @Override
    public List<Address> findByOwnerIdAndOwnerType(Long ownerId, Address.OwnerType ownerType) {
        return repo.findByOwnerIdAndOwnerType(ownerId, ownerType);
    }
}
