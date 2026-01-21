package com.souk.common.adapters.jpa.repository;

import com.souk.common.domain.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByCustomerIdAndStatus(Long customerId, Cart.CartStatus status);

    Optional<Cart> findBySessionIdAndStatus(String sessionId, Cart.CartStatus status);

    List<Cart> findByCustomerId(Long customerId);

    List<Cart> findByStatus(Cart.CartStatus status);
}
