# Redis Configuration for SoukConect E-Commerce Platform

## Overview

This directory contains Redis configuration, initialization scripts, and documentation for the SoukConect platform. Redis is used for:

- **Authentication & Sessions** - Refresh tokens, session management, MFA codes
- **Caching** - User profiles, product details, vendor information
- **Shopping Carts** - User and guest cart data
- **Rate Limiting** - API request throttling
- **Real-time Features** - Online users, notifications
- **Background Jobs** - Email queues, order processing
- **Analytics** - Trending products, metrics

---

## Redis Key Structure

**Convention:** `{namespace}:{entity}:{identifier}:{attribute}`

All keys use **Database 0** with namespace prefixes for organization.

### 1. Authentication Keys (`auth:*`)

| Key Pattern | Type | TTL | Description |
|-------------|------|-----|-------------|
| `auth:refresh_token:{uuid}` | String (JSON) | 7 days | Refresh token data |
| `auth:blacklist:access_token:{jti}` | String | 15 min | Blacklisted JWT tokens (logout) |
| `auth:email_verification:{token}` | String (JSON) | 24 hours | Email verification tokens |
| `auth:password_reset:{token}` | String (JSON) | 1 hour | Password reset tokens |
| `auth:failed_login:{email}` | String (counter) | 15 min | Failed login attempt counter |
| `auth:sessions:{user_id}` | Set | 30 days | Active session IDs per user |
| `auth:mfa:code:{user_id}` | String | 5 min | MFA verification code |

**Example:**
```
auth:refresh_token:550e8400-e29b-41d4-9876-1a2b3c4d5e6f
Value: {"user_id":100,"issued_at":"2026-01-14T10:00:00Z","device":"Chrome/120.0"}
```

---

### 2. Cache Keys (`cache:*`)

| Key Pattern | Type | TTL | Description |
|-------------|------|-----|-------------|
| `cache:user:profile:{user_id}` | String (JSON) | 1 hour | User profile with roles |
| `cache:user:roles:{user_id}` | Set | 1 hour | User role list |
| `cache:vendor:details:{vendor_id}` | String (JSON) | 30 min | Vendor details |
| `cache:product:details:{product_id}` | String (JSON) | 30 min | Product details |
| `cache:categories` | String (JSON) | 1 hour | Category/subcategory list |
| `cache:homepage:{version}` | String (JSON) | 10 min | Homepage data bundle |

---

### 3. Shopping Cart Keys (`cart:*`)

| Key Pattern | Type | TTL | Description |
|-------------|------|-----|-------------|
| `cart:{user_id}` | Hash | 7 days | User cart (product_id → quantity) |
| `cart:guest:{session_id}` | Hash | 24 hours | Guest cart |

**Example:**
```bash
HGETALL cart:100
# Returns:
# "1001" "2"   (product_id 1001, quantity 2)
# "1002" "1"   (product_id 1002, quantity 1)
```

---

### 4. Order Keys (`order:*`)

| Key Pattern | Type | TTL | Description |
|-------------|------|-----|-------------|
| `order:details:{order_id}` | String (JSON) | 1 hour | Order cache |
| `order:customer:{customer_id}` | Sorted Set | 30 min | Orders by customer (score=timestamp) |
| `order:vendor:{vendor_id}` | Sorted Set | 30 min | Orders by vendor |
| `order:status:{order_id}` | String (JSON) | 24 hours | Order status tracking |

---

### 5. Vendor Keys (`vendor:*`)

| Key Pattern | Type | TTL | Description |
|-------------|------|-----|-------------|
| `vendor:category:{category}` | Set | 15 min | Vendor IDs in category |
| `vendor:active:{vendor_id}` | String (JSON) | 5 min | Vendor online status |
| `vendor:geo:locations` | Geospatial | Persistent | Vendor lat/long coordinates |

**Geospatial Example:**
```bash
# Add vendor location
GEOADD vendor:geo:locations 77.5946 12.9716 50

# Find vendors within 5km radius
GEORADIUS vendor:geo:locations 77.6 12.97 5 km
```

---

### 6. Product Keys (`product:*`)

| Key Pattern | Type | TTL | Description |
|-------------|------|-----|-------------|
| `product:inventory:{product_id}` | String (JSON) | 5 min | Available stock count |
| `product:vendor:{vendor_id}` | Set | 15 min | Product IDs by vendor |
| `product:category:{cat}:{subcat}` | Sorted Set | 15 min | Products by category (score=popularity) |
| `product:search:{query_hash}` | List | 10 min | Cached search results |

---

### 7. Rate Limit Keys (`ratelimit:*`)

| Key Pattern | Type | TTL | Description |
|-------------|------|-----|-------------|
| `ratelimit:user:{user_id}:{endpoint}:{window}` | String (counter) | Window duration | Request count per window |
| `ratelimit:ip:{ip}:{endpoint}:{window}` | String (counter) | Window duration | Request count by IP |

**Example:**
```bash
# Check login rate limit (max 5 attempts per minute)
INCR ratelimit:ip:192.168.1.1:/api/login:1min
EXPIRE ratelimit:ip:192.168.1.1:/api/login:1min 60
```

---

### 8. Analytics Keys (`analytics:*`)

| Key Pattern | Type | TTL | Description |
|-------------|------|-----|-------------|
| `analytics:trending:products:{period}` | Sorted Set | 1 hour | Trending products (score=view count) |
| `analytics:product:views:{product_id}:{date}` | String (counter) | 30 days | Daily view count |
| `analytics:vendor:metrics:{vendor_id}:{metric}:{date}` | String (JSON) | 7 days | Vendor performance metrics |

---

### 9. Queue Keys (`queue:*`)

| Key Pattern | Type | TTL | Description |
|-------------|------|-----|-------------|
| `queue:email:{priority}` | List | Persistent | Email send queue (high/medium/low) |
| `queue:order:processing` | List | Persistent | Order processing queue |

---

### 10. Lock Keys (`lock:*`)

| Key Pattern | Type | TTL | Description |
|-------------|------|-----|-------------|
| `lock:inventory:{product_id}` | String | 30 sec | Prevent overselling during checkout |
| `lock:order:create:{user_id}` | String | 10 sec | Prevent duplicate order submission |

---

### 11. Real-time Keys (`realtime:*`)

| Key Pattern | Type | TTL | Description |
|-------------|------|-----|-------------|
| `realtime:online_users` | Sorted Set | Persistent | Online users (score=last_active timestamp) |
| `realtime:vendor:online:{vendor_id}` | String (JSON) | 5 min | Vendor online status |
| `realtime:notification:{user_id}` | List | 7 days | User notification queue |

---

### 12. Configuration Keys (`config:*`)

| Key Pattern | Type | TTL | Description |
|-------------|------|-----|-------------|
| `config:ratelimit` | Hash | Persistent | Rate limit thresholds |
| `config:cache` | Hash | Persistent | Cache TTL settings |
| `config:features` | Hash | Persistent | Feature flags |

**Example:**
```bash
HGETALL config:ratelimit
# Returns:
# "login_attempts_per_minute" "5"
# "api_requests_per_minute" "100"

HGETALL config:features
# Returns:
# "enable_mfa" "true"
# "maintenance_mode" "false"
```

---

## Connection Modes

### 1. Standalone Mode (Development)

Single Redis instance for local development.

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: ""
    ssl:
      enabled: false
```

### 2. SSL/TLS Mode (Production)

Encrypted connections for production environments.

```yaml
spring:
  redis:
    host: redis.example.com
    port: 6380
    password: ${REDIS_PASSWORD}
    username: ${REDIS_USERNAME}
    ssl:
      enabled: true
```

### 3. Cluster Mode (Sharding)

Distributed Redis with automatic sharding across multiple nodes.

```yaml
spring:
  redis:
    cluster:
      nodes: node1:6379,node2:6379,node3:6379
      max-redirects: 3
    password: ${REDIS_PASSWORD}
```

### 4. Sentinel Mode (High Availability)

Automatic failover with Redis Sentinel.

```yaml
spring:
  redis:
    sentinel:
      master: mymaster
      nodes: sentinel1:26379,sentinel2:26379
      password: ${REDIS_PASSWORD}
```

---

## Authentication

### Redis 5 and Earlier (Password Only)

```yaml
spring:
  redis:
    password: your-secure-password
```

### Redis 6+ (ACL with Username)

```yaml
spring:
  redis:
    username: default
    password: your-secure-password
```

### AWS ElastiCache / Azure Redis Cache

```yaml
spring:
  redis:
    host: your-cache.redis.cache.windows.net
    port: 6380
    password: ${REDIS_PRIMARY_KEY}
    ssl:
      enabled: true
```

---

## Setup Instructions

### Local Development (No SSL)

1. Start Redis via Docker:
```bash
docker run -d -p 6379:6379 --name redis redis:7-alpine
```

2. Test connection:
```bash
redis-cli PING
```

### Local Development (With SSL - Testing)

1. Generate SSL certificates:
```bash
cd redis-config
chmod +x generate-ssl-certs.sh
./generate-ssl-certs.sh
```

2. Start Redis with SSL:
```bash
docker-compose -f docker-compose-redis-ssl.yml up -d
```

3. Test SSL connection:
```bash
redis-cli -h localhost -p 6380 --tls \
    --cert redis-ssl-certs/redis.crt \
    --key redis-ssl-certs/redis.key \
    --cacert redis-ssl-certs/ca.crt \
    -a your-secure-password \
    PING
```

### Production Deployment

Set environment variables:

```bash
export REDIS_HOST=your-redis-prod.example.com
export REDIS_PORT=6380
export REDIS_PASSWORD=your-secure-production-password
export REDIS_USERNAME=default
export REDIS_SSL_ENABLED=true
```

---

## Redis Initialization

Redis is automatically initialized on application startup via `RedisInitializer.java`.

**What gets initialized:**
1. Configuration keys (`config:*`)
2. Persistent data structures (geospatial indexes, queues)
3. Connection health check

**Manual initialization:**
```bash
cd redis-config
chmod +x redis-init.sh
./redis-init.sh
```

---

## Monitoring & Health Checks

### Spring Boot Actuator

Redis health is exposed via Spring Boot Actuator:

```bash
curl http://localhost:8080/actuator/health
```

Response:
```json
{
  "status": "UP",
  "components": {
    "redis": {
      "status": "UP",
      "details": {
        "keys": 1234,
        "memory": "2.5M"
      }
    }
  }
}
```

### Redis CLI Commands

```bash
# Check connection
redis-cli PING

# Get key count
redis-cli DBSIZE

# View memory usage
redis-cli INFO memory

# List keys by pattern
redis-cli KEYS "auth:*"

# Monitor real-time commands
redis-cli MONITOR

# Get slow queries
redis-cli SLOWLOG GET 10
```

---

## Best Practices

### 1. Key Naming
- Use consistent namespace prefixes
- Use lowercase with colons as separators
- Include entity identifiers in keys
- Never use spaces or special characters

### 2. TTL Management
- Always set TTL for temporary data
- Use constants from `RedisTTL.java` class
- Monitor keys without TTL: `redis-cli --scan --pattern "*" | xargs redis-cli TTL | grep -1`

### 3. Memory Management
- Set `maxmemory` policy: `redis-cli CONFIG SET maxmemory-policy allkeys-lru`
- Monitor memory usage regularly
- Use compression for large values (JSON)

### 4. Security
- Enable SSL/TLS in production
- Use strong passwords (min 32 characters)
- Use Redis 6+ ACL for fine-grained access control
- Disable dangerous commands: `CONFIG`, `FLUSHDB`, `FLUSHALL`, `KEYS`

### 5. Performance
- Use pipelining for bulk operations
- Use connection pooling
- Avoid `KEYS` command in production (use `SCAN` instead)
- Use Redis Cluster for horizontal scaling

---

## Troubleshooting

### Connection Refused
```bash
# Check if Redis is running
docker ps | grep redis

# Check firewall
telnet redis-host 6379
```

### SSL Connection Issues
```bash
# Verify certificates
openssl x509 -in redis-ssl-certs/redis.crt -text -noout

# Test SSL handshake
openssl s_client -connect redis-host:6380
```

### Memory Issues
```bash
# Check memory usage
redis-cli INFO memory

# Find largest keys
redis-cli --bigkeys

# Set memory limit
redis-cli CONFIG SET maxmemory 2gb
```

### Slow Performance
```bash
# Check slow queries
redis-cli SLOWLOG GET 10

# Monitor latency
redis-cli --latency

# Check connection pool
# Review lettuce pool settings in application.yml
```

---

## Additional Resources

- [Redis Official Documentation](https://redis.io/documentation)
- [Spring Data Redis Reference](https://docs.spring.io/spring-data/redis/docs/current/reference/html/)
- [Lettuce Redis Client](https://lettuce.io/)
- [Redis Best Practices](https://redis.io/topics/best-practices)

---

## Support

For issues or questions:
1. Check application logs: `tail -f logs/application.log`
2. Check Redis logs: `docker logs redis`
3. Review this documentation
4. Contact DevOps team
