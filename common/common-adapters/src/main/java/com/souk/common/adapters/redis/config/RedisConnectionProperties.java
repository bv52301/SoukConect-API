package com.souk.common.adapters.redis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Redis connection configuration properties.
 * Maps to spring.redis.* in application.yml
 */
@Configuration
@ConfigurationProperties(prefix = "spring.redis")
public class RedisConnectionProperties {

    private String host = "localhost";
    private int port = 6379;
    private String password;
    private String username;  // Redis 6+ ACL username
    private int database = 0;
    private long timeout = 2000;

    private SslProperties ssl = new SslProperties();
    private LettuceProperties lettuce = new LettuceProperties();
    private ClusterProperties cluster = new ClusterProperties();
    private SentinelProperties sentinel = new SentinelProperties();

    // ============================================
    // SSL/TLS Configuration
    // ============================================
    public static class SslProperties {
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    // ============================================
    // Lettuce Connection Pool Configuration
    // ============================================
    public static class LettuceProperties {
        private PoolProperties pool = new PoolProperties();
        private long shutdownTimeout = 100;

        public static class PoolProperties {
            private int maxActive = 50;
            private int maxIdle = 20;
            private int minIdle = 10;
            private long maxWait = 2000;

            public int getMaxActive() {
                return maxActive;
            }

            public void setMaxActive(int maxActive) {
                this.maxActive = maxActive;
            }

            public int getMaxIdle() {
                return maxIdle;
            }

            public void setMaxIdle(int maxIdle) {
                this.maxIdle = maxIdle;
            }

            public int getMinIdle() {
                return minIdle;
            }

            public void setMinIdle(int minIdle) {
                this.minIdle = minIdle;
            }

            public long getMaxWait() {
                return maxWait;
            }

            public void setMaxWait(long maxWait) {
                this.maxWait = maxWait;
            }
        }

        public PoolProperties getPool() {
            return pool;
        }

        public void setPool(PoolProperties pool) {
            this.pool = pool;
        }

        public long getShutdownTimeout() {
            return shutdownTimeout;
        }

        public void setShutdownTimeout(long shutdownTimeout) {
            this.shutdownTimeout = shutdownTimeout;
        }
    }

    // ============================================
    // Redis Cluster Configuration
    // ============================================
    public static class ClusterProperties {
        private String nodes;  // Comma-separated list
        private int maxRedirects = 3;

        public String getNodes() {
            return nodes;
        }

        public void setNodes(String nodes) {
            this.nodes = nodes;
        }

        public String[] getNodesArray() {
            return nodes != null && !nodes.isEmpty() ? nodes.split(",") : new String[0];
        }

        public int getMaxRedirects() {
            return maxRedirects;
        }

        public void setMaxRedirects(int maxRedirects) {
            this.maxRedirects = maxRedirects;
        }
    }

    // ============================================
    // Redis Sentinel Configuration
    // ============================================
    public static class SentinelProperties {
        private String master;
        private String nodes;  // Comma-separated list
        private String password;

        public String getMaster() {
            return master;
        }

        public void setMaster(String master) {
            this.master = master;
        }

        public String getNodes() {
            return nodes;
        }

        public void setNodes(String nodes) {
            this.nodes = nodes;
        }

        public String[] getNodesArray() {
            return nodes != null && !nodes.isEmpty() ? nodes.split(",") : new String[0];
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    // ============================================
    // Main Property Getters & Setters
    // ============================================

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getDatabase() {
        return database;
    }

    public void setDatabase(int database) {
        this.database = database;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public SslProperties getSsl() {
        return ssl;
    }

    public void setSsl(SslProperties ssl) {
        this.ssl = ssl;
    }

    public LettuceProperties getLettuce() {
        return lettuce;
    }

    public void setLettuce(LettuceProperties lettuce) {
        this.lettuce = lettuce;
    }

    public ClusterProperties getCluster() {
        return cluster;
    }

    public void setCluster(ClusterProperties cluster) {
        this.cluster = cluster;
    }

    public SentinelProperties getSentinel() {
        return sentinel;
    }

    public void setSentinel(SentinelProperties sentinel) {
        this.sentinel = sentinel;
    }
}
