package com.souk.common.adapters.redis.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SslOptions;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis configuration with support for:
 * - SSL/TLS encryption
 * - Username/password authentication (Redis 6+ ACL)
 * - Standalone, Cluster, and Sentinel modes
 * - Connection pooling
 * - JSON serialization
 */
@Configuration
public class RedisConfig {

    @Autowired
    private RedisConnectionProperties redisProperties;

    /**
     * Main Redis connection factory.
     * Automatically detects and configures Standalone, Cluster, or Sentinel mode.
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        if (isClusterMode()) {
            return createClusterConnectionFactory();
        } else if (isSentinelMode()) {
            return createSentinelConnectionFactory();
        } else {
            return createStandaloneConnectionFactory();
        }
    }

    /**
     * Create Standalone Redis connection factory (default mode).
     */
    private LettuceConnectionFactory createStandaloneConnectionFactory() {
        RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration();
        standaloneConfig.setHostName(redisProperties.getHost());
        standaloneConfig.setPort(redisProperties.getPort());
        standaloneConfig.setDatabase(redisProperties.getDatabase());

        // Set password if provided
        if (redisProperties.getPassword() != null && !redisProperties.getPassword().isEmpty()) {
            standaloneConfig.setPassword(RedisPassword.of(redisProperties.getPassword()));
        }

        // Set username if provided (Redis 6+ ACL)
        if (redisProperties.getUsername() != null && !redisProperties.getUsername().isEmpty()) {
            standaloneConfig.setUsername(redisProperties.getUsername());
        }

        LettuceConnectionFactory factory = new LettuceConnectionFactory(
            standaloneConfig,
            createLettuceClientConfiguration()
        );

        factory.setShareNativeConnection(true);
        factory.setValidateConnection(true);

        return factory;
    }

    /**
     * Create Redis Cluster connection factory.
     */
    private LettuceConnectionFactory createClusterConnectionFactory() {
        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration();

        // Add cluster nodes
        for (String node : redisProperties.getCluster().getNodesArray()) {
            String[] hostPort = node.trim().split(":");
            if (hostPort.length == 2) {
                clusterConfig.addClusterNode(
                    new RedisNode(hostPort[0], Integer.parseInt(hostPort[1]))
                );
            }
        }

        clusterConfig.setMaxRedirects(redisProperties.getCluster().getMaxRedirects());

        // Set password
        if (redisProperties.getPassword() != null && !redisProperties.getPassword().isEmpty()) {
            clusterConfig.setPassword(RedisPassword.of(redisProperties.getPassword()));
        }

        // Set username (Redis 6+ ACL)
        if (redisProperties.getUsername() != null && !redisProperties.getUsername().isEmpty()) {
            clusterConfig.setUsername(redisProperties.getUsername());
        }

        return new LettuceConnectionFactory(
            clusterConfig,
            createLettuceClientConfiguration()
        );
    }

    /**
     * Create Redis Sentinel connection factory (High Availability).
     */
    private LettuceConnectionFactory createSentinelConnectionFactory() {
        RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration();
        sentinelConfig.setMaster(redisProperties.getSentinel().getMaster());

        // Add sentinel nodes
        for (String node : redisProperties.getSentinel().getNodesArray()) {
            String[] hostPort = node.trim().split(":");
            if (hostPort.length == 2) {
                sentinelConfig.addSentinel(
                    new RedisNode(hostPort[0], Integer.parseInt(hostPort[1]))
                );
            }
        }

        sentinelConfig.setDatabase(redisProperties.getDatabase());

        // Redis password (for data nodes)
        if (redisProperties.getPassword() != null && !redisProperties.getPassword().isEmpty()) {
            sentinelConfig.setPassword(RedisPassword.of(redisProperties.getPassword()));
        }

        // Sentinel password (for sentinel nodes)
        if (redisProperties.getSentinel().getPassword() != null
            && !redisProperties.getSentinel().getPassword().isEmpty()) {
            sentinelConfig.setSentinelPassword(
                RedisPassword.of(redisProperties.getSentinel().getPassword())
            );
        }

        // Username (Redis 6+ ACL)
        if (redisProperties.getUsername() != null && !redisProperties.getUsername().isEmpty()) {
            sentinelConfig.setUsername(redisProperties.getUsername());
        }

        return new LettuceConnectionFactory(
            sentinelConfig,
            createLettuceClientConfiguration()
        );
    }

    /**
     * Create Lettuce client configuration with SSL and pooling.
     */
    private LettuceClientConfiguration createLettuceClientConfiguration() {
        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder =
            LettucePoolingClientConfiguration.builder();

        // Connection timeout
        builder.commandTimeout(Duration.ofMillis(redisProperties.getTimeout()));

        // Shutdown timeout
        builder.shutdownTimeout(
            Duration.ofMillis(redisProperties.getLettuce().getShutdownTimeout())
        );

        // Connection pool configuration
        builder.poolConfig(createPoolConfig());

        // SSL/TLS Configuration
        if (redisProperties.getSsl().isEnabled()) {
            SslOptions sslOptions = SslOptions.builder()
                .jdkSslProvider()  // Use JDK SSL provider
                .build();

            ClientOptions clientOptions = ClientOptions.builder()
                .sslOptions(sslOptions)
                .build();

            builder.clientOptions(clientOptions);
            builder.useSsl();  // Enable SSL
        }

        return builder.build();
    }

    /**
     * Create connection pool configuration.
     * Using raw type to avoid generic type mismatch with Lettuce client configuration.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private GenericObjectPoolConfig createPoolConfig() {
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();

        var poolProps = redisProperties.getLettuce().getPool();

        poolConfig.setMaxTotal(poolProps.getMaxActive());
        poolConfig.setMaxIdle(poolProps.getMaxIdle());
        poolConfig.setMinIdle(poolProps.getMinIdle());
        poolConfig.setMaxWaitMillis(poolProps.getMaxWait());

        // Test connections
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(false);
        poolConfig.setTestWhileIdle(true);

        // Eviction policy
        poolConfig.setTimeBetweenEvictionRunsMillis(60000); // 1 minute
        poolConfig.setMinEvictableIdleTimeMillis(300000);   // 5 minutes
        poolConfig.setNumTestsPerEvictionRun(3);

        return poolConfig;
    }

    /**
     * RedisTemplate with JSON serialization for objects.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // JSON serialization for values
        Jackson2JsonRedisSerializer<Object> serializer =
            new Jackson2JsonRedisSerializer<>(Object.class);

        // String serialization for keys
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(serializer);

        template.setEnableTransactionSupport(true);
        template.afterPropertiesSet();

        return template;
    }

    /**
     * RedisTemplate with String serialization (for counters, flags, simple values).
     */
    @Bean
    public RedisTemplate<String, String> stringRedisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);

        template.afterPropertiesSet();

        return template;
    }

    /**
     * Check if Cluster mode is configured.
     */
    private boolean isClusterMode() {
        return redisProperties.getCluster() != null
            && redisProperties.getCluster().getNodes() != null
            && !redisProperties.getCluster().getNodes().isEmpty();
    }

    /**
     * Check if Sentinel mode is configured.
     */
    private boolean isSentinelMode() {
        return redisProperties.getSentinel() != null
            && redisProperties.getSentinel().getMaster() != null
            && !redisProperties.getSentinel().getMaster().isEmpty();
    }
}
