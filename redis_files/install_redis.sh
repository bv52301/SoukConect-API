#!/usr/bin/env bash
# =============================================================================
# Redis Installation and Initialization Script for SoukConect
# =============================================================================
#
# Purpose: Initialize Redis with configuration keys, feature flags, and
#          data structures needed by the SoukConect application.
#
# All keys are prefixed with "soukconect:" for namespace isolation.
#
# Usage:
#   ./install_redis.sh [environment]
#
# Arguments:
#   environment - Optional: dev, staging, prod (default: dev)
#
# Environment Variables:
#   REDIS_HOST     - Redis host (default: localhost)
#   REDIS_PORT     - Redis port (default: 6379)
#   REDIS_PASSWORD - Redis password (optional)
#   REDIS_USERNAME - Redis username for ACL (optional, Redis 6+)
#   REDIS_SSL      - Enable SSL: true/false (default: false)
#
# Examples:
#   # Development (local Redis, no authentication)
#   ./install_redis.sh dev
#
#   # Staging (with password)
#   REDIS_PASSWORD=mypassword ./install_redis.sh staging
#
#   # Production (with SSL and username/password)
#   REDIS_HOST=redis-prod.example.com \
#   REDIS_PORT=6380 \
#   REDIS_USERNAME=soukconect \
#   REDIS_PASSWORD=secure-password \
#   REDIS_SSL=true \
#   ./install_redis.sh prod
#
# =============================================================================

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Key prefix for namespace isolation
KEY_PREFIX="soukconect"

# Default values
ENVIRONMENT="${1:-dev}"
REDIS_HOST="${REDIS_HOST:-localhost}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"
REDIS_USERNAME="${REDIS_USERNAME:-}"
REDIS_SSL="${REDIS_SSL:-false}"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Redis Installation for SoukConect${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "Environment: ${GREEN}${ENVIRONMENT}${NC}"
echo -e "Redis Host:  ${GREEN}${REDIS_HOST}${NC}"
echo -e "Redis Port:  ${GREEN}${REDIS_PORT}${NC}"
echo -e "SSL Enabled: ${GREEN}${REDIS_SSL}${NC}"
echo -e "Key Prefix:  ${GREEN}${KEY_PREFIX}:*${NC}"
echo ""

# Build redis-cli command
REDIS_CLI="redis-cli -h ${REDIS_HOST} -p ${REDIS_PORT}"

# Add SSL support if enabled
if [ "${REDIS_SSL}" = "true" ]; then
    REDIS_CLI="${REDIS_CLI} --tls --insecure"
fi

# Add authentication if provided
if [ -n "${REDIS_PASSWORD}" ]; then
    REDIS_CLI="${REDIS_CLI} -a ${REDIS_PASSWORD}"
    # Add username if using Redis 6+ ACL
    if [ -n "${REDIS_USERNAME}" ]; then
        REDIS_CLI="${REDIS_CLI} --user ${REDIS_USERNAME}"
    fi
fi

# Test connection
echo -e "${YELLOW}Testing Redis connection...${NC}"
if ! ${REDIS_CLI} PING > /dev/null 2>&1; then
    echo -e "${RED}ERROR: Cannot connect to Redis at ${REDIS_HOST}:${REDIS_PORT}${NC}"
    echo -e "${RED}Please check that Redis is running and credentials are correct${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Connected to Redis${NC}"
echo ""

# Get Redis version
REDIS_VERSION=$(${REDIS_CLI} INFO server | grep redis_version | cut -d: -f2 | tr -d '\r')
echo -e "Redis Version: ${GREEN}${REDIS_VERSION}${NC}"
echo ""

# Initialize configuration keys
echo -e "${YELLOW}Initializing configuration keys...${NC}"

# Rate limit configurations
${REDIS_CLI} SET "${KEY_PREFIX}:config:rate_limit:login:max_attempts" "5"
${REDIS_CLI} SET "${KEY_PREFIX}:config:rate_limit:login:window_seconds" "300"
${REDIS_CLI} SET "${KEY_PREFIX}:config:rate_limit:api:max_requests" "1000"
${REDIS_CLI} SET "${KEY_PREFIX}:config:rate_limit:api:window_seconds" "60"

# Cache TTL configurations (in seconds)
${REDIS_CLI} SET "${KEY_PREFIX}:config:cache:user_profile:ttl" "3600"      # 1 hour
${REDIS_CLI} SET "${KEY_PREFIX}:config:cache:vendor_details:ttl" "1800"     # 30 minutes
${REDIS_CLI} SET "${KEY_PREFIX}:config:cache:product_details:ttl" "1800"    # 30 minutes
${REDIS_CLI} SET "${KEY_PREFIX}:config:cache:category_list:ttl" "7200"      # 2 hours

# Session configurations
${REDIS_CLI} SET "${KEY_PREFIX}:config:session:refresh_token:ttl" "604800"  # 7 days
${REDIS_CLI} SET "${KEY_PREFIX}:config:session:access_token:ttl" "3600"     # 1 hour
${REDIS_CLI} SET "${KEY_PREFIX}:config:session:email_verification:ttl" "86400"  # 24 hours

# Feature flags
${REDIS_CLI} SET "${KEY_PREFIX}:config:features:recommendations_enabled" "true"
${REDIS_CLI} SET "${KEY_PREFIX}:config:features:realtime_inventory_enabled" "true"
${REDIS_CLI} SET "${KEY_PREFIX}:config:features:analytics_enabled" "true"
${REDIS_CLI} SET "${KEY_PREFIX}:config:features:cache_enabled" "true"

echo -e "${GREEN}✓ Configuration keys initialized${NC}"
echo ""

# Initialize persistent data structures
echo -e "${YELLOW}Initializing persistent data structures...${NC}"

# Initialize sorted sets for trending products
${REDIS_CLI} ZADD "${KEY_PREFIX}:analytics:trending:products:today" 0 "placeholder" > /dev/null
${REDIS_CLI} ZREM "${KEY_PREFIX}:analytics:trending:products:today" "placeholder" > /dev/null

# Initialize sets for online users
${REDIS_CLI} SADD "${KEY_PREFIX}:realtime:online_users" "placeholder" > /dev/null
${REDIS_CLI} SREM "${KEY_PREFIX}:realtime:online_users" "placeholder" > /dev/null

# Initialize geospatial index for vendor locations
${REDIS_CLI} GEOADD "${KEY_PREFIX}:vendor:locations:geo" 0 0 "placeholder" > /dev/null
${REDIS_CLI} ZREM "${KEY_PREFIX}:vendor:locations:geo" "placeholder" > /dev/null

echo -e "${GREEN}✓ Data structures initialized${NC}"
echo ""

# Environment-specific initialization
case "${ENVIRONMENT}" in
    dev)
        echo -e "${YELLOW}Development environment setup...${NC}"
        ${REDIS_CLI} SET "${KEY_PREFIX}:config:environment" "development"
        ${REDIS_CLI} SET "${KEY_PREFIX}:config:debug:enabled" "true"
        ${REDIS_CLI} SET "${KEY_PREFIX}:config:cache:aggressive" "false"
        echo -e "${GREEN}✓ Development configuration applied${NC}"
        ;;
    staging)
        echo -e "${YELLOW}Staging environment setup...${NC}"
        ${REDIS_CLI} SET "${KEY_PREFIX}:config:environment" "staging"
        ${REDIS_CLI} SET "${KEY_PREFIX}:config:debug:enabled" "true"
        ${REDIS_CLI} SET "${KEY_PREFIX}:config:cache:aggressive" "true"
        echo -e "${GREEN}✓ Staging configuration applied${NC}"
        ;;
    prod)
        echo -e "${YELLOW}Production environment setup...${NC}"
        ${REDIS_CLI} SET "${KEY_PREFIX}:config:environment" "production"
        ${REDIS_CLI} SET "${KEY_PREFIX}:config:debug:enabled" "false"
        ${REDIS_CLI} SET "${KEY_PREFIX}:config:cache:aggressive" "true"
        # Set production-specific configurations
        ${REDIS_CLI} SET "${KEY_PREFIX}:config:monitoring:enabled" "true"
        ${REDIS_CLI} SET "${KEY_PREFIX}:config:logging:level" "WARN"
        echo -e "${GREEN}✓ Production configuration applied${NC}"
        ;;
    *)
        echo -e "${YELLOW}Unknown environment: ${ENVIRONMENT}, using defaults${NC}"
        ${REDIS_CLI} SET "${KEY_PREFIX}:config:environment" "${ENVIRONMENT}"
        ;;
esac
echo ""

# Display summary
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Redis Initialization Complete${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Get key count for soukconect prefix
KEY_COUNT=$(${REDIS_CLI} --scan --pattern "${KEY_PREFIX}:*" | wc -l | tr -d ' ')
echo -e "Total keys with ${KEY_PREFIX}: prefix: ${GREEN}${KEY_COUNT}${NC}"
echo ""

# Show some key patterns
echo -e "${YELLOW}Initialized key patterns:${NC}"
echo -e "  • ${KEY_PREFIX}:config:*      - Configuration keys"
echo -e "  • ${KEY_PREFIX}:analytics:*   - Analytics data"
echo -e "  • ${KEY_PREFIX}:realtime:*    - Real-time features"
echo -e "  • ${KEY_PREFIX}:vendor:*      - Vendor data"
echo ""

echo -e "${GREEN}✓ Redis is ready for use!${NC}"
echo ""

# Show next steps
echo -e "${YELLOW}Next steps:${NC}"
echo -e "  1. Start your SoukConect services"
echo -e "  2. Check Redis health at: http://localhost:8080/actuator/health"
echo -e "  3. Monitor Redis: redis-cli -h ${REDIS_HOST} -p ${REDIS_PORT} MONITOR"
echo -e "  4. List all keys: redis-cli KEYS \"${KEY_PREFIX}:*\""
echo ""
