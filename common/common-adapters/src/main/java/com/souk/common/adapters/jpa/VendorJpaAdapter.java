package com.souk.common.adapters.jpa;

import com.souk.common.domain.Vendor;
import com.souk.common.port.DataAccessPort;
import com.souk.common.adapters.jpa.repository.VendorRepository;
import org.springframework.stereotype.Component;

@Component
public class VendorJpaAdapter extends JpaDataAccessAdapter<Vendor, Long>
        implements DataAccessPort<Vendor, Long> {

    public VendorJpaAdapter(VendorRepository repo) {
        super(repo);
    }
}

