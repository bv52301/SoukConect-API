#!/bin/bash

# ============================================================
# Redis Initialization Script for SoukConect
# ============================================================
#
# PURPOSE: Initialize Redis with configuration keys and
#          persistent data structures
#
# USAGE:
#   ./redis-init.sh
#
# ENVIRONMENT VARIABLES:
#   REDIS_HOST     - Redis host (default: localhost)
#   REDIS_PORT     - Redis port (default: 6379)
#   REDIS_PASSWORD - Redis password (optional)
#   REDIS_USERNAME - Redis username for ACL (optional, Redis 6+)
#
# ============================================================

set -e

# Configuration
REDIS_HOST=${REDIS_HOST:-localhost}
REDIS_PORT=${REDIS_PORT:-6379}
REDIS_PASSWORD=${REDIS_PASSWORD:-}
REDIS_USERNAME=${REDIS_USERNAME:-}

echo "============================================"
echo "Redis Initialization for SoukConect"
echo "============================================"
echo "Host: $REDIS_HOST"
echo "Port: $REDIS_PORT"
echo "Username: ${REDIS_USERNAME:-<none>}"
echo "Password: ${REDIS_PASSWORD:+***}"
echo "============================================"
echo ""

# Build Redis CLI command
REDIS_CLI="redis-cli -h $REDIS_HOST -p $REDIS_PORT"

if [ -n "$REDIS_PASSWORD" ]; then
    REDIS_CLI="$REDIS_CLI -a $REDIS_PASSWORD"
fi

if [ -n "$REDIS_USERNAME" ]; then
    REDIS_CLI="$REDIS_CLI --user $REDIS_USERNAME"
fi

# Test connection
echo "Testing Redis connection..."
if $REDIS_CLI PING > /dev/null 2>&1; then
    echo "✓ Redis connection successful"
else
    echo "✗ Redis connection failed!"
    echo "Please check your Redis connection settings."
    exit 1
fi

echo ""
echo "Initializing configuration keys..."

# Initialize rate limit configuration
$REDIS_CLI HSET config:ratelimit login_attempts_per_minute 5
$REDIS_CLI HSET config:ratelimit api_requests_per_minute 100
$REDIS_CLI HSET config:ratelimit order_creation_per_hour 10
$REDIS_CLI HSET config:ratelimit search_requests_per_minute 30
echo "✓ Rate limit configuration set"

# Initialize cache TTL configuration (in seconds)
$REDIS_CLI HSET config:cache user_profile_ttl 3600
$REDIS_CLI HSET config:cache product_details_ttl 1800
$REDIS_CLI HSET config:cache vendor_details_ttl 1800
$REDIS_CLI HSET config:cache category_list_ttl 3600
$REDIS_CLI HSET config:cache homepage_ttl 600
echo "✓ Cache TTL configuration set"

# Initialize feature flags
$REDIS_CLI HSET config:features enable_mfa true
$REDIS_CLI HSET config:features enable_guest_checkout true
$REDIS_CLI HSET config:features maintenance_mode false
$REDIS_CLI HSET config:features enable_product_reviews true
$REDIS_CLI HSET config:features enable_vendor_chat false
echo "✓ Feature flags set"

# Initialize persistent data structures
echo ""
echo "Initializing persistent data structures..."

# Check if vendor geospatial index exists
if [ $($REDIS_CLI EXISTS vendor:geo:locations) -eq 0 ]; then
    echo "✓ Vendor geospatial index ready (will be populated by application)"
else
    VENDOR_COUNT=$($REDIS_CLI ZCARD vendor:geo:locations)
    echo "✓ Vendor geospatial index exists ($VENDOR_COUNT vendors)"
fi

# Check online users sorted set
if [ $($REDIS_CLI EXISTS realtime:online_users) -eq 0 ]; then
    echo "✓ Online users tracking ready"
else
    ONLINE_COUNT=$($REDIS_CLI ZCARD realtime:online_users)
    echo "✓ Online users tracking exists ($ONLINE_COUNT users)"
fi

# Initialize email queues (if they don't exist)
for PRIORITY in high medium low; do
    QUEUE_KEY="queue:email:$PRIORITY"
    if [ $($REDIS_CLI EXISTS $QUEUE_KEY) -eq 0 ]; then
        echo "✓ Email queue initialized: $PRIORITY"
    else
        QUEUE_SIZE=$($REDIS_CLI LLEN $QUEUE_KEY)
        echo "✓ Email queue exists: $PRIORITY ($QUEUE_SIZE items)"
    fi
done

# Initialize order processing queue
if [ $($REDIS_CLI EXISTS queue:order:processing) -eq 0 ]; then
    echo "✓ Order processing queue ready"
else
    ORDER_QUEUE_SIZE=$($REDIS_CLI LLEN queue:order:processing)
    echo "✓ Order processing queue exists ($ORDER_QUEUE_SIZE items)"
fi

echo ""
echo "============================================"
echo "Redis Initialization Summary"
echo "============================================"

# Get database statistics
TOTAL_KEYS=$($REDIS_CLI DBSIZE)
MEMORY_INFO=$($REDIS_CLI INFO memory | grep used_memory_human | cut -d: -f2 | tr -d '\r')

echo "Total Keys: $TOTAL_KEYS"
echo "Memory Used: $MEMORY_INFO"

# Count keys by namespace
echo ""
echo "Keys by Namespace:"
for NAMESPACE in auth cache cart order vendor product ratelimit analytics queue lock realtime config; do
    COUNT=$($REDIS_CLI KEYS "${NAMESPACE}:*" | wc -l)
    if [ $COUNT -gt 0 ]; then
        echo "  $NAMESPACE: $COUNT"
    fi
done

echo ""
echo "============================================"
echo "✓ Redis initialization complete!"
echo "============================================"
echo ""
echo "Next steps:"
echo "  1. Start your Spring Boot application"
echo "  2. RedisInitializer will run automatically on startup"
echo "  3. Monitor Redis: redis-cli -h $REDIS_HOST -p $REDIS_PORT MONITOR"
echo "  4. Check health: curl http://localhost:8080/actuator/health"
echo ""
