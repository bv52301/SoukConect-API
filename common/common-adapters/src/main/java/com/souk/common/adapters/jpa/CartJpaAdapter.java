package com.souk.common.adapters.jpa;

import com.souk.common.domain.Cart;
import com.souk.common.port.DataAccessPort;
import com.souk.common.port.CartQueryPort;
import com.souk.common.adapters.jpa.repository.CartRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CartJpaAdapter extends JpaDataAccessAdapter<Cart, Long>
        implements DataAccessPort<Cart, Long>, CartQueryPort {

    private final CartRepository repo;

    public CartJpaAdapter(CartRepository repo) {
        super(repo);
        this.repo = repo;
    }

    @Override
    public Optional<Cart> findActiveCartByCustomerId(Long customerId) {
        return repo.findByCustomerIdAndStatus(customerId, Cart.CartStatus.ACTIVE);
    }

    @Override
    public Optional<Cart> findActiveCartBySessionId(String sessionId) {
        return repo.findBySessionIdAndStatus(sessionId, Cart.CartStatus.ACTIVE);
    }

    @Override
    public List<Cart> findByCustomerId(Long customerId) {
        return repo.findByCustomerId(customerId);
    }

    @Override
    public List<Cart> findByStatus(Cart.CartStatus status) {
        return repo.findByStatus(status);
    }
}
