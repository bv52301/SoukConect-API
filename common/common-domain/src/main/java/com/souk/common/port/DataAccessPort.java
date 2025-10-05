package com.souk.common.port;

import java.util.*;

public interface DataAccessPort<T, ID> {
    T save(T entity);
    Optional<T> findById(ID id);
    List<T> findAll();
    void deleteById(ID id);
}
