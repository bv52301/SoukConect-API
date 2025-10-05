// common-adapters/.../es/ElasticSearchAdapter.java
package com.souk.common.adapters.es;

import com.souk.common.port.SearchPort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ElasticSearchAdapter<T> implements SearchPort<T> {
    public interface Mapper<T> { T fromHit(Object hit); }
    private final Object esClient; // your client type
    private final String index;
    private final Mapper<T> mapper;

    public ElasticSearchAdapter(Object esClient, String index, Mapper<T> mapper) {
        this.esClient = esClient; this.index = index; this.mapper = mapper;
    }

    @Override public List<T> search(String q, int limit) {
        // build query on `index`, map hits via mapper
        return List.of();
    }
}
