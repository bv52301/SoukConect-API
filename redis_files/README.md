# Redis Installation Files for SoukConect

Source directory for Redis initialization scripts and configuration files.

## Contents

- **install_redis.sh** - Main installation script (Linux/Unix)
- **install_redis.bat** - Main installation script (Windows)
- **redis-init.sh** - Manual Redis initialization (alternative method)
- **generate-ssl-certs.sh** - Generate SSL certificates for testing
- **docker-compose-redis-ssl.yml** - Docker Compose for Redis with SSL
- **application-redis-sample.yml** - Sample Spring Boot Redis configuration
- **redis-keys-reference.json** - Machine-readable reference of all Redis key patterns

## Quick Start

### Development (Local Redis)
```bash
# Linux/Unix
./install_redis.sh dev

# Windows
install_redis.bat dev
```

### Production (with SSL and authentication)
```bash
# Linux/Unix
REDIS_HOST=redis-prod.example.com \
REDIS_PORT=6380 \
REDIS_USERNAME=default \
REDIS_PASSWORD=secure-password \
REDIS_SSL=true \
./install_redis.sh prod

# Windows
set REDIS_HOST=redis-prod.example.com
set REDIS_PORT=6380
set REDIS_USERNAME=default
set REDIS_PASSWORD=secure-password
install_redis.bat prod
```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `REDIS_HOST` | Redis server hostname | `localhost` |
| `REDIS_PORT` | Redis server port | `6379` |
| `REDIS_PASSWORD` | Redis password | (none) |
| `REDIS_USERNAME` | Redis username (Redis 6+ ACL) | (none) |
| `REDIS_SSL` | Enable SSL: `true`/`false` | `false` |

## What Gets Initialized

The installation script sets up:

### 1. Configuration Keys
```
config:rate_limit:login:max_attempts        = 5
config:rate_limit:login:window_seconds      = 300
config:cache:user_profile:ttl               = 3600 (1 hour)
config:cache:vendor_details:ttl             = 1800 (30 min)
config:session:refresh_token:ttl            = 604800 (7 days)
config:features:cache_enabled               = true
... and more
```

### 2. Data Structures
- **Trending products** - Sorted sets for analytics
- **Online users** - Sets for real-time tracking
- **Vendor geospatial index** - For location-based queries

### 3. Environment-Specific Settings
- **dev**: Debug enabled, less aggressive caching
- **staging**: Debug enabled, aggressive caching
- **prod**: Debug disabled, monitoring enabled

## Verification

After installation:
```bash
# Check connection
redis-cli PING

# Get key count
redis-cli DBSIZE

# List configuration keys
redis-cli KEYS "config:*"

# Monitor real-time
redis-cli MONITOR
```

## Spring Boot Integration

Copy `application-redis-sample.yml` to your service's `src/main/resources/application.yml`:

```yaml
spring:
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    ssl:
      enabled: false
```

## Testing with Docker

Use the included Docker Compose file for local testing with SSL:

```bash
# Generate test certificates
./generate-ssl-certs.sh

# Start Redis with SSL
docker-compose -f docker-compose-redis-ssl.yml up -d

# Test connection
redis-cli -h localhost -p 6380 --tls \
  --cert redis-ssl-certs/redis.crt \
  --key redis-ssl-certs/redis.key \
  --cacert redis-ssl-certs/ca.crt \
  -a your-password PING
```

## Key Patterns Reference

See `redis-keys-reference.json` for complete list of all key patterns used by SoukConect:

- `auth:*` - Authentication & sessions
- `cache:*` - Application caching
- `cart:*` - Shopping carts
- `order:*` - Orders
- `vendor:*` - Vendor data
- `product:*` - Product catalog
- `ratelimit:*` - API rate limiting
- `analytics:*` - Metrics and analytics
- `queue:*` - Background jobs
- `lock:*` - Distributed locks
- `realtime:*` - Real-time features
- `config:*` - Configuration keys

## Notes

- These are SOURCE files - do not ship this directory directly to production
- Always build using the `db-installer` Maven module which packages these files
- The built ZIP includes proper line endings for each platform
- For production, use proper SSL certificates (not self-signed test certificates)
- Redis initialization is idempotent - safe to run multiple times
- Configuration keys can be updated at runtime via redis-cli

## Troubleshooting

### Cannot connect to Redis
```bash
# Check if Redis is running
redis-cli PING

# Check with Docker
docker ps | grep redis

# Check logs
docker logs redis
```

### SSL connection issues
```bash
# Verify certificates
openssl x509 -in redis-ssl-certs/redis.crt -text -noout

# Test SSL connection
openssl s_client -connect localhost:6380
```

### Authentication errors
```bash
# Test with password
redis-cli -a your-password PING

# Test with username (Redis 6+)
redis-cli --user default -a your-password PING
```

## Additional Resources

- [Redis Documentation](https://redis.io/documentation)
- [Spring Data Redis](https://docs.spring.io/spring-data/redis/docs/current/reference/html/)
- [Lettuce Client](https://lettuce.io/)
- [Redis Best Practices](https://redis.io/topics/best-practices)
