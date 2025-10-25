package com.souk.customer.api;

import com.souk.common.domain.Customer;
import com.souk.common.port.DataAccessPort;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final DataAccessPort<Customer, Long> customerPort;

    public CustomerController(DataAccessPort<Customer, Long> customerPort) {
        this.customerPort = customerPort;
    }

    @GetMapping
    public List<Customer> listAll() {
        return customerPort.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> getById(@PathVariable @Min(1) Long id) {
        return customerPort.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Customer> create(@RequestBody @Valid Customer customer) {
        Customer saved = customerPort.save(customer);
        return ResponseEntity.created(URI.create("/customers/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Customer> update(@PathVariable @Min(1) Long id,
                                           @RequestBody @Valid Customer updated) {
        return customerPort.findById(id)
                .map(existing -> {
                    existing.setFirstName(updated.getFirstName());
                    existing.setLastName(updated.getLastName());
                    existing.setEmail(updated.getEmail());
                    existing.setPhone(updated.getPhone());
                    existing.setAddresses(updated.getAddresses());
                    return ResponseEntity.ok(customerPort.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Min(1) Long id) {
        return customerPort.findById(id)
                .map(c -> {
                    customerPort.deleteById(id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().<Void>build());
    }
}