package com.souk.common.adapters.jpa;

import com.souk.common.adapters.jpa.repository.VendorLocationRepository;
import com.souk.common.domain.VendorLocation;
import com.souk.common.port.VendorLocationQueryPort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VendorLocationJpaAdapter extends JpaDataAccessAdapter<VendorLocation, Long>
        implements VendorLocationQueryPort {

    private final VendorLocationRepository repo;

    public VendorLocationJpaAdapter(VendorLocationRepository repo) {
        super(repo);
        this.repo = repo;
    }

    @Override
    public List<VendorLocation> findByVendorId(Long vendorId) {
        return repo.findByVendorId(vendorId);
    }

    @Override
    public List<VendorLocation> findByVendorIdAndStatus(Long vendorId, VendorLocation.Status status) {
        return repo.findByVendorIdAndStatus(vendorId, status);
    }
}
