package com.souk.common.adapters.jpa;

import com.souk.common.domain.OrderItem;
import com.souk.common.port.DataAccessPort;
import com.souk.common.adapters.jpa.repository.OrderItemRepository;
import org.springframework.stereotype.Component;

@Component
public class OrderItemJpaAdapter extends JpaDataAccessAdapter<OrderItem, Long>
        implements DataAccessPort<OrderItem, Long> {

    public OrderItemJpaAdapter(OrderItemRepository repo) {
        super(repo);
    }
}

