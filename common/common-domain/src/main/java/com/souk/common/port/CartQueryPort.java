package com.souk.common.port;

import com.souk.common.domain.Cart;
import java.util.List;
import java.util.Optional;

/**
 * Domain-specific query port for Cart lookups.
 */
public interface CartQueryPort {

    Optional<Cart> findActiveCartByCustomerId(Long customerId);

    Optional<Cart> findActiveCartBySessionId(String sessionId);

    List<Cart> findByCustomerId(Long customerId);

    List<Cart> findByStatus(Cart.CartStatus status);
}
