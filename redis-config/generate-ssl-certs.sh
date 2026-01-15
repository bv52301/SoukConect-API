#!/bin/bash

# ============================================================
# Redis SSL Certificate Generation Script
# ============================================================
#
# PURPOSE: Generate self-signed SSL certificates for Redis
#          testing and development
#
# USAGE:
#   ./generate-ssl-certs.sh
#
# OUTPUT:
#   redis-ssl-certs/
#     ├── ca.crt       - Certificate Authority certificate
#     ├── ca.key       - Certificate Authority private key
#     ├── redis.crt    - Redis server certificate
#     ├── redis.key    - Redis server private key
#     └── redis.csr    - Certificate signing request
#
# NOTE: These are self-signed certificates for development only.
#       For production, use certificates from a trusted CA.
#
# ============================================================

set -e

echo "============================================"
echo "Redis SSL Certificate Generator"
echo "============================================"
echo ""

# Create directory for certificates
CERT_DIR="redis-ssl-certs"
mkdir -p $CERT_DIR
cd $CERT_DIR

echo "Generating certificates in: $(pwd)"
echo ""

# Configuration
DAYS_VALID=3650  # 10 years
COUNTRY="US"
STATE="California"
CITY="San Francisco"
ORG="SoukConect"
CN_CA="Redis-CA"
CN_SERVER="redis-server"

# Step 1: Generate Certificate Authority (CA)
echo "Step 1: Generating Certificate Authority (CA)..."
openssl genrsa -out ca.key 4096
openssl req -new -x509 -days $DAYS_VALID -key ca.key -out ca.crt \
    -subj "/C=$COUNTRY/ST=$STATE/L=$CITY/O=$ORG/CN=$CN_CA"
echo "✓ CA generated: ca.crt, ca.key"
echo ""

# Step 2: Generate Redis server private key
echo "Step 2: Generating Redis server private key..."
openssl genrsa -out redis.key 2048
echo "✓ Redis server key generated: redis.key"
echo ""

# Step 3: Generate Certificate Signing Request (CSR)
echo "Step 3: Generating Certificate Signing Request (CSR)..."
openssl req -new -key redis.key -out redis.csr \
    -subj "/C=$COUNTRY/ST=$STATE/L=$CITY/O=$ORG/CN=$CN_SERVER"
echo "✓ CSR generated: redis.csr"
echo ""

# Step 4: Sign the certificate with CA
echo "Step 4: Signing Redis server certificate with CA..."
openssl x509 -req -days $DAYS_VALID -in redis.csr -CA ca.crt -CAkey ca.key \
    -CAcreateserial -out redis.crt
echo "✓ Redis server certificate signed: redis.crt"
echo ""

# Step 5: Set proper permissions
echo "Step 5: Setting file permissions..."
chmod 600 redis.key ca.key
chmod 644 redis.crt ca.crt redis.csr
echo "✓ Permissions set"
echo ""

# Display certificate information
echo "============================================"
echo "Certificate Information"
echo "============================================"
echo ""

echo "Certificate Authority (CA):"
openssl x509 -in ca.crt -noout -subject -dates
echo ""

echo "Redis Server Certificate:"
openssl x509 -in redis.crt -noout -subject -dates
echo ""

# Display file list
echo "============================================"
echo "Generated Files"
echo "============================================"
ls -lh
echo ""

# Verification
echo "============================================"
echo "Verification"
echo "============================================"
echo "Verifying Redis certificate against CA..."
if openssl verify -CAfile ca.crt redis.crt > /dev/null 2>&1; then
    echo "✓ Certificate verification successful!"
else
    echo "✗ Certificate verification failed!"
    exit 1
fi
echo ""

# Usage instructions
echo "============================================"
echo "Usage Instructions"
echo "============================================"
echo ""
echo "1. Start Redis with SSL:"
echo "   redis-server --port 0 --tls-port 6380 \\"
echo "     --tls-cert-file $(pwd)/redis.crt \\"
echo "     --tls-key-file $(pwd)/redis.key \\"
echo "     --tls-ca-cert-file $(pwd)/ca.crt \\"
echo "     --tls-auth-clients no"
echo ""
echo "2. Or use Docker Compose:"
echo "   docker-compose -f docker-compose-redis-ssl.yml up -d"
echo ""
echo "3. Test connection:"
echo "   redis-cli -h localhost -p 6380 --tls \\"
echo "     --cert $(pwd)/redis.crt \\"
echo "     --key $(pwd)/redis.key \\"
echo "     --cacert $(pwd)/ca.crt \\"
echo "     PING"
echo ""
echo "4. For Spring Boot, enable SSL in application.yml:"
echo "   spring:"
echo "     redis:"
echo "       host: localhost"
echo "       port: 6380"
echo "       ssl:"
echo "         enabled: true"
echo ""
echo "============================================"
echo "✓ Certificate generation complete!"
echo "============================================"
echo ""
echo "NOTE: These are self-signed certificates for development."
echo "      For production, use certificates from a trusted CA."
echo ""

cd ..
