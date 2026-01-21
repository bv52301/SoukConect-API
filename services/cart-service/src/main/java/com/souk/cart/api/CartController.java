package com.souk.cart.api;

import com.souk.cart.api.dto.AddItemRequest;
import com.souk.cart.api.dto.CartResponse;
import com.souk.cart.api.dto.UpdateItemRequest;
import com.souk.cart.service.CartService;
import com.souk.common.domain.Cart;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    // --- Get cart for logged-in customer ---
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<CartResponse> getCustomerCart(@PathVariable @Min(1) Long customerId) {
        Cart cart = cartService.getOrCreateCart(customerId);
        return ResponseEntity.ok(CartResponse.from(cart));
    }

    // --- Get cart for guest session ---
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<CartResponse> getGuestCart(@PathVariable String sessionId) {
        Cart cart = cartService.getOrCreateGuestCart(sessionId);
        return ResponseEntity.ok(CartResponse.from(cart));
    }

    // --- Get cart by ID ---
    @GetMapping("/{cartId}")
    public ResponseEntity<CartResponse> getCartById(@PathVariable @Min(1) Long cartId) {
        return cartService.getCartById(cartId)
                .map(CartResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // --- Add item to cart ---
    @PostMapping("/{cartId}/items")
    public ResponseEntity<CartResponse> addItem(
            @PathVariable @Min(1) Long cartId,
            @Valid @RequestBody AddItemRequest request) {
        try {
            Cart cart = cartService.addItem(cartId, request.productId(), request.quantity());
            return ResponseEntity.created(URI.create("/cart/" + cartId))
                    .body(CartResponse.from(cart));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.unprocessableEntity().build();
        }
    }

    // --- Update item quantity ---
    @PutMapping("/{cartId}/items/{itemId}")
    public ResponseEntity<CartResponse> updateItem(
            @PathVariable @Min(1) Long cartId,
            @PathVariable @Min(1) Long itemId,
            @Valid @RequestBody UpdateItemRequest request) {
        try {
            Cart cart = cartService.updateItemQuantity(cartId, itemId, request.quantity());
            return ResponseEntity.ok(CartResponse.from(cart));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // --- Remove item from cart ---
    @DeleteMapping("/{cartId}/items/{itemId}")
    public ResponseEntity<CartResponse> removeItem(
            @PathVariable @Min(1) Long cartId,
            @PathVariable @Min(1) Long itemId) {
        try {
            Cart cart = cartService.removeItem(cartId, itemId);
            return ResponseEntity.ok(CartResponse.from(cart));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // --- Clear cart ---
    @DeleteMapping("/{cartId}")
    public ResponseEntity<CartResponse> clearCart(@PathVariable @Min(1) Long cartId) {
        try {
            Cart cart = cartService.clearCart(cartId);
            return ResponseEntity.ok(CartResponse.from(cart));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // --- Merge guest cart into customer cart ---
    @PostMapping("/merge")
    public ResponseEntity<CartResponse> mergeCart(
            @RequestParam String sessionId,
            @RequestParam Long customerId) {
        try {
            Cart cart = cartService.mergeGuestCart(sessionId, customerId);
            return ResponseEntity.ok(CartResponse.from(cart));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // --- Convert cart to order (mark as converted) ---
    @PostMapping("/{cartId}/checkout")
    public ResponseEntity<CartResponse> checkout(@PathVariable @Min(1) Long cartId) {
        try {
            Cart cart = cartService.convertCart(cartId);
            return ResponseEntity.ok(CartResponse.from(cart));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
