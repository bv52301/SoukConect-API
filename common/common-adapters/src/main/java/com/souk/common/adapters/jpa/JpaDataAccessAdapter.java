// common-adapters/.../jpa/JpaDataAccessAdapter.java
package com.souk.common.adapters.jpa;

import com.souk.common.port.DataAccessPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public class JpaDataAccessAdapter<T, ID> implements DataAccessPort<T, ID> {
    private final JpaRepository<T, ID> repo;

    public JpaDataAccessAdapter(JpaRepository<T, ID> repo) { this.repo = repo; }

    @Override public T save(T e) { return repo.save(e); }
    @Override public Optional<T> findById(ID id) { return repo.findById(id); }
    @Override public List<T> findAll() { return repo.findAll(); }

    @Override public void deleteById(ID id) { repo.deleteById(id); }
}
