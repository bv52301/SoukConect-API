// common-adapters/.../redis/RedisCacheAdapter.java
package com.souk.common.adapters.redis;

import com.souk.common.port.CachePort;
import java.time.Duration;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "rediscache.enabled", havingValue = "true", matchIfMissing=false)
public class RedisCacheAdapter<T, ID> implements CachePort<T, ID> {
    public interface Serde<T> { String toJson(T v); T fromJson(String s); }
    public interface Keyer<ID> { String key(ID id); }

    private final StringRedisTemplate redis;
    private final Serde<T> serde;
    private final Keyer<ID> keyer;

    public RedisCacheAdapter(StringRedisTemplate redis, Serde<T> serde, Keyer<ID> keyer) {
        this.redis = redis; this.serde = serde; this.keyer = keyer;
    }

    @Override public Optional<T> get(ID id) {
        String json = redis.opsForValue().get(keyer.key(id));
        return json == null ? Optional.empty() : Optional.of(serde.fromJson(json));
    }
    @Override public void put(ID id, T value, Duration ttl) {
        redis.opsForValue().set(keyer.key(id), serde.toJson(value), ttl);
    }
    @Override public void evict(ID id) { redis.delete(keyer.key(id)); }
}
