package com.souk.cuisine.api;

import com.souk.common.domain.Cuisine;
import com.souk.common.domain.CuisineImage;
import com.souk.common.port.DataAccessPort;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/cuisines")
@Validated
public class CuisineController {

    private final DataAccessPort<Cuisine, Long> cuisinePort;
    private final DataAccessPort<CuisineImage, Long> cuisineImagePort;

    public CuisineController(DataAccessPort<Cuisine, Long> cuisinePort,
                             DataAccessPort<CuisineImage, Long> cuisineImagePort) {
        this.cuisinePort = cuisinePort;
        this.cuisineImagePort = cuisineImagePort;
    }

    @GetMapping
    public List<Cuisine> listAll() {
        return cuisinePort.findAll();
    }

    @GetMapping("/categories")
    public List<CategoryWithImage> listDistinctCategories() {
        var imagesByName = cuisineImagePort.findAll().stream()
                .filter(img -> img.getType() == CuisineImage.Type.CATEGORY)
                .filter(img -> img.getName() != null && !img.getName().isBlank())
                .collect(Collectors.toMap(img -> img.getName().toLowerCase(), CuisineImage::getImageUrl, (a, b) -> a));

        return cuisinePort.findAll().stream()
                .map(Cuisine::getCategory)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .sorted(String::compareToIgnoreCase)
                .map(cat -> new CategoryWithImage(cat, imagesByName.getOrDefault(cat.toLowerCase(), "None")))
                .collect(Collectors.toList());
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
                    existing.setImage(req.getImage());
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

    // ------------------------------------------------------------
    // Cuisine Images
    // ------------------------------------------------------------

    @GetMapping("/images")
    public List<CuisineImage> listImages(
            @RequestParam(value = "type", required = false) CuisineImage.Type type,
            @RequestParam(value = "name", required = false) String name) {
        return cuisineImagePort.findAll().stream()
                .filter(img -> type == null || img.getType() == type)
                .filter(img -> name == null || img.getName().equalsIgnoreCase(name))
                .toList();
    }

    @PostMapping("/images")
    public ResponseEntity<CuisineImage> createImage(@RequestBody @Valid CuisineImage req) {
        // Basic guard: name must correspond to an existing cuisine for the given type
        boolean exists = cuisinePort.findAll().stream().anyMatch(c -> {
            return switch (req.getType()) {
                case CUISINE -> c.getCuisineName() != null && c.getCuisineName().equalsIgnoreCase(req.getName());
                case CATEGORY -> c.getCategory() != null && c.getCategory().equalsIgnoreCase(req.getName());
                case SUBCATEGORY -> c.getSubcategory() != null && c.getSubcategory().equalsIgnoreCase(req.getName());
            };
        });
        if (!exists) return ResponseEntity.badRequest().build();

        CuisineImage saved = cuisineImagePort.save(req);
        return ResponseEntity.created(URI.create("/cuisines/images/" + saved.getId())).body(saved);
    }

    @DeleteMapping("/images/{id}")
    public ResponseEntity<Void> deleteImage(@PathVariable @Min(1) Long id) {
        return cuisineImagePort.findById(id)
                .map(img -> { cuisineImagePort.deleteById(id); return ResponseEntity.noContent().<Void>build(); })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/images/{id}")
    public ResponseEntity<CuisineImage> updateImage(@PathVariable @Min(1) Long id,
                                                    @RequestBody @Valid CuisineImage req) {
        return cuisineImagePort.findById(id)
                .map(existing -> {
                    boolean exists = cuisinePort.findAll().stream().anyMatch(c -> {
                        return switch (req.getType()) {
                            case CUISINE -> c.getCuisineName() != null && c.getCuisineName().equalsIgnoreCase(req.getName());
                            case CATEGORY -> c.getCategory() != null && c.getCategory().equalsIgnoreCase(req.getName());
                            case SUBCATEGORY -> c.getSubcategory() != null && c.getSubcategory().equalsIgnoreCase(req.getName());
                        };
                    });
                    if (!exists) return ResponseEntity.badRequest().<CuisineImage>build();
                    existing.setType(req.getType());
                    existing.setName(req.getName());
                    existing.setImageUrl(req.getImageUrl());
                    existing.setDescription(req.getDescription());
                    CuisineImage saved = cuisineImagePort.save(existing);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    public record CategoryWithImage(String category, String imageUrl) {}
}
