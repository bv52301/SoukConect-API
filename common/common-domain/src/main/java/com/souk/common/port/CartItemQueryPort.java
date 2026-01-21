package com.souk.common.port;

import com.souk.common.domain.CartItem;
import java.util.List;
import java.util.Optional;

/**
 * Domain-specific query port for CartItem lookups.
 */
public interface CartItemQueryPort {

    List<CartItem> findByCartId(Long cartId);

    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    void deleteByCartId(Long cartId);
}
