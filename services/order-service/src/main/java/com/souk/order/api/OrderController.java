package com.souk.order.api;

import com.souk.common.domain.Order;
import com.souk.common.domain.Customer;
import com.souk.common.domain.Address;
import com.souk.common.domain.Product;
import com.souk.common.port.DataAccessPort;
import com.souk.order.api.dto.OrderCreateRequest;
import com.souk.order.api.dto.OrderResponse;
import com.souk.order.api.dto.OrderUpdateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final DataAccessPort<Order, Long> orderPort;
    private final DataAccessPort<Customer, Long> customerPort;
    private final DataAccessPort<Address, Long> addressPort;
    private final DataAccessPort<Product, Long> productPort;

    public OrderController(DataAccessPort<Order, Long> orderPort,
            DataAccessPort<Customer, Long> customerPort,
            DataAccessPort<Address, Long> addressPort,
            DataAccessPort<Product, Long> productPort) {
        this.orderPort = orderPort;
        this.customerPort = customerPort;
        this.addressPort = addressPort;
        this.productPort = productPort;
    }

    // --- List all orders ---
    @GetMapping
    public List<OrderResponse> listAll() {
        return orderPort.findAll().stream()
                .map(OrderResponse::from)
                .toList();
    }

    // --- Get order by ID ---
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(@PathVariable @Min(1) Long id) {
        return orderPort.findById(id)
                .map(OrderResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // --- Create new order ---
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody OrderCreateRequest req) {
        if (req.customerId() == null)
            return ResponseEntity.badRequest().body(Map.of("error", "customerId is required"));

        Optional<Customer> customerOpt = customerPort.findById(req.customerId());
        if (customerOpt.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "Customer not found: " + req.customerId()));

        Address address = null;
        if (req.addressId() != null) {
            address = addressPort.findById(req.addressId()).orElse(null);
            if (address == null)
                return ResponseEntity.badRequest().body(Map.of("error", "Address not found: " + req.addressId()));
        }

        if (req.items() != null && !req.items().isEmpty()) {
            List<Long> missingProducts = new ArrayList<>();
            for (OrderCreateRequest.OrderItemRequest item : req.items()) {
                if (item.productId() == null) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Order item is missing productId"));
                }
                if (productPort.findById(item.productId()).isEmpty()) {
                    missingProducts.add(item.productId());
                }
            }
            if (!missingProducts.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Product(s) not found: " + missingProducts));
            }
        }

        Order order = req.toDomain(customerOpt.get(), address);
        Order saved = orderPort.save(order);
        return ResponseEntity.created(URI.create("/orders/" + saved.getId()))
                .body(OrderResponse.from(saved));
    }

    // --- Update order ---
    @PutMapping("/{id}")
    public ResponseEntity<OrderResponse> update(@PathVariable @Min(1) Long id,
            @Valid @RequestBody OrderUpdateRequest req) {
        return orderPort.findById(id)
                .map(existing -> {
                    if (req.status() != null)
                        existing.setStatus(req.status());
                    if (req.paymentMethod() != null)
                        existing.setPaymentMethod(req.paymentMethod());
                    if (req.requestedDeliveryDate() != null)
                        existing.setRequestedDeliveryDate(req.requestedDeliveryDate());
                    if (req.deliveryFlexibility() != null)
                        existing.setDeliveryFlexibility(req.deliveryFlexibility());
                    if (req.deliverySlotStart() != null)
                        existing.setDeliverySlotStart(req.deliverySlotStart());
                    if (req.deliverySlotEnd() != null)
                        existing.setDeliverySlotEnd(req.deliverySlotEnd());
                    if (req.notes() != null)
                        existing.setNotes(req.notes());
                    Order updated = orderPort.save(existing);
                    return ResponseEntity.ok(OrderResponse.from(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // --- Update order status only ---
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable @Min(1) Long id,
            @RequestBody Map<String, String> body) {
        String statusStr = body.get("status");
        if (statusStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "status field is required"));
        }
        Order.OrderStatus status;
        try {
            status = Order.OrderStatus.valueOf(statusStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid status '" + statusStr + "'. Valid values: " +
                            Arrays.toString(Order.OrderStatus.values())));
        }
        return orderPort.findById(id)
                .map(existing -> {
                    existing.setStatus(status);
                    Order updated = orderPort.save(existing);
                    return ResponseEntity.<Object>ok(OrderResponse.from(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // --- Delete order ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Min(1) Long id) {
        if (orderPort.findById(id).isPresent()) {
            orderPort.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
