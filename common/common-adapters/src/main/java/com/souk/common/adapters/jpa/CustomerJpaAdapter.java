package com.souk.common.adapters.jpa;

import com.souk.common.domain.Customer;
import com.souk.common.port.DataAccessPort;
import com.souk.common.adapters.jpa.repository.CustomerRepository;
import org.springframework.stereotype.Component;

@Component
public class CustomerJpaAdapter extends JpaDataAccessAdapter<Customer, Long>
        implements DataAccessPort<Customer, Long> {

    public CustomerJpaAdapter(CustomerRepository repo) {
        super(repo);
    }
}

