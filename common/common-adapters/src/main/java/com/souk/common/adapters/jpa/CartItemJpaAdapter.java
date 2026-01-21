package com.souk.common.adapters.jpa;

import com.souk.common.domain.CartItem;
import com.souk.common.port.DataAccessPort;
import com.souk.common.port.CartItemQueryPort;
import com.souk.common.adapters.jpa.repository.CartItemRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class CartItemJpaAdapter extends JpaDataAccessAdapter<CartItem, Long>
        implements DataAccessPort<CartItem, Long>, CartItemQueryPort {

    private final CartItemRepository repo;

    public CartItemJpaAdapter(CartItemRepository repo) {
        super(repo);
        this.repo = repo;
    }

    @Override
    public List<CartItem> findByCartId(Long cartId) {
        return repo.findByCartId(cartId);
    }

    @Override
    public Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId) {
        return repo.findByCartIdAndProductId(cartId, productId);
    }

    @Override
    @Transactional
    public void deleteByCartId(Long cartId) {
        repo.deleteByCartId(cartId);
    }
}
