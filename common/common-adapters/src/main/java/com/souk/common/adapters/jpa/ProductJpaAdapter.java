package com.souk.common.adapters.jpa;
import com.souk.common.port.DataAccessPort;
import com.souk.common.domain.Product;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import com.souk.common.adapters.jpa.repository.ProductRepository;


@Component
public class ProductJpaAdapter extends JpaDataAccessAdapter<Product, Long> implements DataAccessPort<Product,Long> {
    public ProductJpaAdapter(ProductRepository repo) {
        super(repo);
    }
}
