// common-domain/src/main/java/com/souk/common/port/CachePort.java
package com.souk.common.port;

import java.time.Duration;
import java.util.*;

public interface CachePort<T, ID> {
    Optional<T> get(ID id);
    void put(ID id, T value, Duration ttl);
    void evict(ID id);
}
