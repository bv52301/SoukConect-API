package com.souk.common.adapters.jpa;

import com.souk.common.domain.Order;
import com.souk.common.port.DataAccessPort;
import com.souk.common.adapters.jpa.repository.OrderRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class OrderJpaAdapter extends JpaDataAccessAdapter<Order, Long>
        implements DataAccessPort<Order, Long> {

    private final OrderRepository orderRepo;

    public OrderJpaAdapter(OrderRepository repo) {
        super(repo);
        this.orderRepo = repo;
    }

    @Override
    public Optional<Order> findById(Long id) {
        return orderRepo.findByIdWithItems(id);
    }

    @Override
    public List<Order> findAll() {
        return orderRepo.findAllWithItems();
    }
}

