package com.souk.common.port;

import com.souk.common.domain.Product;
import java.util.Optional;

/**
 * Domain-specific query port for Product lookups that cannot be
 * expressed efficiently via the generic DataAccessPort.
 */
public interface ProductQueryPort {
    Optional<Product> findBySku(String sku);
}

