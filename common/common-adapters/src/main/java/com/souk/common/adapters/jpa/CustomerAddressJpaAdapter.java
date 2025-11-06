package com.souk.common.adapters.jpa;

import com.souk.common.domain.CustomerAddress;
import com.souk.common.port.DataAccessPort;
import com.souk.common.adapters.jpa.repository.CustomerAddressRepository;
import org.springframework.stereotype.Component;

@Component
public class CustomerAddressJpaAdapter extends JpaDataAccessAdapter<CustomerAddress, Long>
        implements DataAccessPort<CustomerAddress, Long> {

    public CustomerAddressJpaAdapter(CustomerAddressRepository repo) {
        super(repo);
    }
}

