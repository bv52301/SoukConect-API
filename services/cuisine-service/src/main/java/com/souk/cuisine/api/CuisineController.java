package com.souk.cuisine.api;

import com.souk.common.domain.Cuisine;
import com.souk.common.port.DataAccessPort;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/cuisines")
@Validated
public class CuisineController {

    private final DataAccessPort<Cuisine, Long> cuisinePort;

    public CuisineController(DataAccessPort<Cuisine, Long> cuisinePort) {
        this.cuisinePort = cuisinePort;
    }

    @GetMapping
    public List<Cuisine> listAll() {
        return cuisinePort.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cuisine> getById(@PathVariable @Min(1) Long id) {
        return cuisinePort.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Cuisine> create(@RequestBody @Valid Cuisine req) {
        Cuisine saved = cuisinePort.save(req);
        return ResponseEntity.created(URI.create("/cuisines/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cuisine> update(@PathVariable @Min(1) Long id, @RequestBody @Valid Cuisine req) {
        return cuisinePort.findById(id)
                .map(existing -> {
                    existing.setCuisineName(req.getCuisineName());
                    existing.setCategory(req.getCategory());
                    existing.setSubcategory(req.getSubcategory());
                    existing.setRegion(req.getRegion());
                    return ResponseEntity.ok(cuisinePort.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Min(1) Long id) {
        return cuisinePort.findById(id)
                .map(c -> { cuisinePort.deleteById(id); return ResponseEntity.noContent().<Void>build(); })
                .orElse(ResponseEntity.notFound().build());
    }
}

