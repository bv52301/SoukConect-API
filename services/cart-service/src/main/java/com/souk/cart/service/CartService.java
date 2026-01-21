package com.souk.cart.service;

import com.souk.common.domain.Cart;
import com.souk.common.domain.CartItem;
import com.souk.common.domain.Customer;
import com.souk.common.domain.Product;
import com.souk.common.port.CartQueryPort;
import com.souk.common.port.CartItemQueryPort;
import com.souk.common.port.DataAccessPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CartService {

    private final DataAccessPort<Cart, Long> cartPort;
    private final DataAccessPort<CartItem, Long> cartItemPort;
    private final DataAccessPort<Product, Long> productPort;
    private final DataAccessPort<Customer, Long> customerPort;
    private final CartQueryPort cartQueryPort;
    private final CartItemQueryPort cartItemQueryPort;

    public CartService(
            DataAccessPort<Cart, Long> cartPort,
            DataAccessPort<CartItem, Long> cartItemPort,
            DataAccessPort<Product, Long> productPort,
            DataAccessPort<Customer, Long> customerPort,
            CartQueryPort cartQueryPort,
            CartItemQueryPort cartItemQueryPort) {
        this.cartPort = cartPort;
        this.cartItemPort = cartItemPort;
        this.productPort = productPort;
        this.customerPort = customerPort;
        this.cartQueryPort = cartQueryPort;
        this.cartItemQueryPort = cartItemQueryPort;
    }

    /**
     * Get or create active cart for a customer.
     */
    @Transactional
    public Cart getOrCreateCart(Long customerId) {
        return cartQueryPort.findActiveCartByCustomerId(customerId)
                .orElseGet(() -> {
                    Customer customer = customerPort.findById(customerId)
                            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
                    Cart cart = new Cart();
                    cart.setCustomer(customer);
                    cart.setStatus(Cart.CartStatus.ACTIVE);
                    return cartPort.save(cart);
                });
    }

    /**
     * Get or create active cart for a guest session.
     */
    @Transactional
    public Cart getOrCreateGuestCart(String sessionId) {
        return cartQueryPort.findActiveCartBySessionId(sessionId)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setSessionId(sessionId);
                    cart.setStatus(Cart.CartStatus.ACTIVE);
                    return cartPort.save(cart);
                });
    }

    /**
     * Get cart by ID.
     */
    public Optional<Cart> getCartById(Long cartId) {
        return cartPort.findById(cartId);
    }

    /**
     * Add item to cart. If product already exists, update quantity.
     */
    @Transactional
    public Cart addItem(Long cartId, Long productId, int quantity) {
        Cart cart = cartPort.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found: " + cartId));

        Product product = productPort.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        if (!product.getAvailable()) {
            throw new IllegalStateException("Product is not available: " + productId);
        }

        // Check if item already exists in cart
        Optional<CartItem> existingItem = cartItemQueryPort.findByCartIdAndProductId(cartId, productId);

        if (existingItem.isPresent()) {
            // Update quantity
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            cartItemPort.save(item);
        } else {
            // Add new item
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setUnitPrice(product.getPrice());
            cartItemPort.save(item);
        }

        return cartPort.findById(cartId).orElse(cart);
    }

    /**
     * Update item quantity.
     */
    @Transactional
    public Cart updateItemQuantity(Long cartId, Long itemId, int quantity) {
        Cart cart = cartPort.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found: " + cartId));

        CartItem item = cartItemPort.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found: " + itemId));

        if (!item.getCart().getId().equals(cartId)) {
            throw new IllegalArgumentException("Item does not belong to this cart");
        }

        item.setQuantity(quantity);
        cartItemPort.save(item);

        return cartPort.findById(cartId).orElse(cart);
    }

    /**
     * Remove item from cart.
     */
    @Transactional
    public Cart removeItem(Long cartId, Long itemId) {
        Cart cart = cartPort.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found: " + cartId));

        CartItem item = cartItemPort.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found: " + itemId));

        if (!item.getCart().getId().equals(cartId)) {
            throw new IllegalArgumentException("Item does not belong to this cart");
        }

        cartItemPort.deleteById(itemId);

        return cartPort.findById(cartId).orElse(cart);
    }

    /**
     * Clear all items from cart.
     */
    @Transactional
    public Cart clearCart(Long cartId) {
        Cart cart = cartPort.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found: " + cartId));

        cartItemQueryPort.deleteByCartId(cartId);

        return cartPort.findById(cartId).orElse(cart);
    }

    /**
     * Mark cart as converted (checkout completed).
     */
    @Transactional
    public Cart convertCart(Long cartId) {
        Cart cart = cartPort.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found: " + cartId));

        cart.setStatus(Cart.CartStatus.CONVERTED);
        return cartPort.save(cart);
    }

    /**
     * Merge guest cart into customer cart after login.
     */
    @Transactional
    public Cart mergeGuestCart(String sessionId, Long customerId) {
        Optional<Cart> guestCartOpt = cartQueryPort.findActiveCartBySessionId(sessionId);
        if (guestCartOpt.isEmpty()) {
            return getOrCreateCart(customerId);
        }

        Cart guestCart = guestCartOpt.get();
        Cart customerCart = getOrCreateCart(customerId);

        // Merge items from guest cart to customer cart
        for (CartItem guestItem : guestCart.getItems()) {
            Optional<CartItem> existingItem = cartItemQueryPort.findByCartIdAndProductId(
                    customerCart.getId(), guestItem.getProduct().getId());

            if (existingItem.isPresent()) {
                // Add quantities
                CartItem item = existingItem.get();
                item.setQuantity(item.getQuantity() + guestItem.getQuantity());
                cartItemPort.save(item);
            } else {
                // Move item to customer cart
                CartItem newItem = new CartItem();
                newItem.setCart(customerCart);
                newItem.setProduct(guestItem.getProduct());
                newItem.setQuantity(guestItem.getQuantity());
                newItem.setUnitPrice(guestItem.getUnitPrice());
                cartItemPort.save(newItem);
            }
        }

        // Mark guest cart as merged
        guestCart.setStatus(Cart.CartStatus.MERGED);
        cartPort.save(guestCart);

        return cartPort.findById(customerCart.getId()).orElse(customerCart);
    }
}
