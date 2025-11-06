package com.souk.common.adapters.jpa;
import com.souk.common.port.DataAccessPort;
import com.souk.common.port.ProductQueryPort;
import com.souk.common.domain.Product;
import org.springframework.stereotype.Component;
import com.souk.common.adapters.jpa.repository.ProductRepository;

import java.util.Optional;

@Component
public class ProductJpaAdapter extends JpaDataAccessAdapter<Product, Long>
        implements DataAccessPort<Product, Long>, ProductQueryPort {

    private final ProductRepository repo;

    public ProductJpaAdapter(ProductRepository repo) {
        super(repo);
        this.repo = repo;
    }

    @Override
    public Optional<Product> findBySku(String sku) {
        return repo.findBySku(sku);
    }
}
