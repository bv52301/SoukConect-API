package com.souk.common.adapters.jpa;

import com.souk.common.domain.Order;
import com.souk.common.port.DataAccessPort;
import com.souk.common.adapters.jpa.repository.OrderRepository;
import org.springframework.stereotype.Component;

@Component
public class OrderJpaAdapter extends JpaDataAccessAdapter<Order, Long>
        implements DataAccessPort<Order, Long> {

    public OrderJpaAdapter(OrderRepository repo) {
        super(repo);
    }
}

