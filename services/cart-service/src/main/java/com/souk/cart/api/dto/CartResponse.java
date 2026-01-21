package com.souk.cart.api.dto;

import com.souk.common.domain.Cart;
import com.souk.common.domain.CartItem;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CartResponse(
        Long cartId,
        Long customerId,
        String sessionId,
        String status,
        List<CartItemResponse> items,
        int totalItems,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CartResponse from(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems() != null
                ? cart.getItems().stream().map(CartItemResponse::from).toList()
                : List.of();

        return new CartResponse(
                cart.getId(),
                cart.getCustomer() != null ? cart.getCustomer().getId() : null,
                cart.getSessionId(),
                cart.getStatus().name(),
                itemResponses,
                cart.getTotalItems(),
                cart.getTotalAmount(),
                cart.getCreatedAt(),
                cart.getUpdatedAt()
        );
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CartItemResponse(
            Long cartItemId,
            Long productId,
            String productName,
            String productSku,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal,
            LocalDateTime addedAt,
            LocalDateTime updatedAt
    ) {
        public static CartItemResponse from(CartItem item) {
            return new CartItemResponse(
                    item.getId(),
                    item.getProduct() != null ? item.getProduct().getId() : null,
                    item.getProduct() != null ? item.getProduct().getName() : null,
                    item.getProduct() != null ? item.getProduct().getSku() : null,
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getSubtotal(),
                    item.getAddedAt(),
                    item.getUpdatedAt()
            );
        }
    }
}
