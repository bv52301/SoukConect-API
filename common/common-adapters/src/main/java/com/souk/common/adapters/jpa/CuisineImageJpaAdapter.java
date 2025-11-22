package com.souk.common.adapters.jpa;

import com.souk.common.adapters.jpa.repository.CuisineImageRepository;
import com.souk.common.domain.CuisineImage;
import com.souk.common.port.DataAccessPort;
import org.springframework.stereotype.Component;

@Component
public class CuisineImageJpaAdapter extends JpaDataAccessAdapter<CuisineImage, Long>
        implements DataAccessPort<CuisineImage, Long> {

    public CuisineImageJpaAdapter(CuisineImageRepository repo) {
        super(repo);
    }
}
