package com.souk.order.api.dto;

import com.souk.common.domain.Order;
import com.souk.common.domain.OrderItem;
import com.souk.common.domain.Customer;
import com.souk.common.domain.CustomerAddress;
import com.souk.common.port.DataAccessPort;
import com.souk.order.api.dto.OrderCreateRequest;
import com.souk.order.api.dto.OrderResponse;
import com.souk.order.api.dto.OrderUpdateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final DataAccessPort<Order, Long> orderPort;
    private final DataAccessPort<Customer, Long> customerPort;
    private final DataAccessPort<CustomerAddress, Long> addressPort;

    public OrderController(DataAccessPort<Order, Long> orderPort,
                           DataAccessPort<Customer, Long> customerPort,
                           DataAccessPort<CustomerAddress, Long> addressPort) {
        this.orderPort = orderPort;
        this.customerPort = customerPort;
        this.addressPort = addressPort;
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
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderCreateRequest req) {
        Optional<Customer> customerOpt = customerPort.findById(req.customerId());
        if (customerOpt.isEmpty()) return ResponseEntity.badRequest().build();

        Order order = req.toDomain(customerOpt.get(), req.addressId() != null
                ? addressPort.findById(req.addressId()).orElse(null)
                : null);

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
                    existing.setStatus(req.status());
                    existing.setPaymentMethod(req.paymentMethod());
                    existing.setRequestedDeliveryDate(req.requestedDeliveryDate());
                    existing.setDeliveryFlexibility(req.deliveryFlexibility());
                    existing.setDeliverySlotStart(req.deliverySlotStart());
                    existing.setDeliverySlotEnd(req.deliverySlotEnd());
                    existing.setNotes(req.notes());
                    Order updated = orderPort.save(existing);
                    return ResponseEntity.ok(OrderResponse.from(updated));
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
