package com.souk.common.adapters.jpa;

import com.souk.common.domain.Cuisine;
import com.souk.common.adapters.jpa.repository.CuisineRepository;
import com.souk.common.port.DataAccessPort;
import org.springframework.stereotype.Component;

@Component
public class CuisineJpaAdapter extends JpaDataAccessAdapter<Cuisine, Long> implements DataAccessPort<Cuisine, Long> {
    public CuisineJpaAdapter(CuisineRepository repo) { super(repo); }
}

