package com.souk.common.adapters.jpa;

import com.souk.common.domain.ProductMedia;
import com.souk.common.port.DataAccessPort;
import com.souk.common.adapters.jpa.repository.ProductMediaRepository;
import org.springframework.stereotype.Component;

@Component
public class ProductMediaJpaAdapter extends JpaDataAccessAdapter<ProductMedia, Long>
        implements DataAccessPort<ProductMedia, Long> {

    public ProductMediaJpaAdapter(ProductMediaRepository repo) {
        super(repo);
    }
}

