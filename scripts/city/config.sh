#!/usr/bin/env bash
#
# Central settings for the City service deployed to Payara.
# Keep portable defaults here; put real account paths next to it in config.local.sh.
#

# Payara installation and domain.
PAYARA_HOME="${PAYARA_HOME:-/opt/payara6}"
PAYARA_DOMAIN="${PAYARA_DOMAIN:-domain1}"
PAYARA_ADMIN_PORT="${PAYARA_ADMIN_PORT:-4848}"

# City HTTPS endpoint.
CITY_SERVICE_HOST="${CITY_SERVICE_HOST:-localhost}"
CITY_SERVICE_PORT="${CITY_SERVICE_PORT:-8181}"

# City application and already-built WAR.
CITY_SERVICE_CONTEXT_ROOT="${CITY_SERVICE_CONTEXT_ROOT:-city-service}"
CITY_WAR_RELATIVE_PATH="${CITY_WAR_RELATIVE_PATH:-city-service/target/city-service-1.0.0.war}"

# PostgreSQL/Hibernate. Leave them empty until database credentials are supplied.
# Values are installed as Payara JVM system properties by configure-payara.sh.
CITY_DB_URL="${CITY_DB_URL:-}"
CITY_DB_USER="${CITY_DB_USER:-}"
CITY_DB_PASSWORD="${CITY_DB_PASSWORD:-}"
CITY_DB_SCHEMA_MODE="${CITY_DB_SCHEMA_MODE:-update}"

# Payara server certificate.
PAYARA_KEYSTORE_FILE="${PAYARA_KEYSTORE_FILE:-keystore.p12}"

# HTTPS certificate and Java memory settings.
STORE_PASSWORD="${STORE_PASSWORD:-changeit}"
CITY_CERTIFICATE_ALIAS="${CITY_CERTIFICATE_ALIAS:-city-service}"
CERTIFICATE_DNS_NAME="${CERTIFICATE_DNS_NAME:-localhost}"
CERTIFICATE_IP_ADDRESS="${CERTIFICATE_IP_ADDRESS:-127.0.0.1}"

# Memory limits for Java tools and application servers. Required on hosts where
# the default JVM ergonomics exceed a per-user memory quota.
JAVA_INITIAL_HEAP="${JAVA_INITIAL_HEAP:-32m}"
JAVA_MAX_HEAP="${JAVA_MAX_HEAP:-192m}"
JAVA_ACTIVE_PROCESSORS="${JAVA_ACTIVE_PROCESSORS:-1}"
JAVA_COMPRESSED_CLASS_SPACE="${JAVA_COMPRESSED_CLASS_SPACE:-64m}"
JAVA_MAX_METASPACE="${JAVA_MAX_METASPACE:-192m}"
JAVA_THREAD_STACK="${JAVA_THREAD_STACK:-256k}"

# Keep Payara worker pools small on shared university hosts.
PAYARA_HTTP_MAX_THREADS="${PAYARA_HTTP_MAX_THREADS:-4}"
PAYARA_ADMIN_MAX_THREADS="${PAYARA_ADMIN_MAX_THREADS:-2}"
PAYARA_ORB_MAX_THREADS="${PAYARA_ORB_MAX_THREADS:-4}"
PAYARA_THREAD_QUEUE_SIZE="${PAYARA_THREAD_QUEUE_SIZE:-32}"

# Load private account-specific values.
SOA_CONFIG_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=config.local.sh
source "$SOA_CONFIG_DIR/config.local.sh"
unset SOA_CONFIG_DIR

# Build derived paths and the API address after all overrides are loaded.
refresh_soa_config() {
  PAYARA_CONFIG_DIR="$PAYARA_HOME/glassfish/domains/$PAYARA_DOMAIN/config"
  PAYARA_LOG_DIR="$PAYARA_HOME/glassfish/domains/$PAYARA_DOMAIN/logs"
  PAYARA_KEYSTORE="$PAYARA_CONFIG_DIR/$PAYARA_KEYSTORE_FILE"
  CITY_SERVICE_BASE_URL="https://$CITY_SERVICE_HOST:$CITY_SERVICE_PORT/$CITY_SERVICE_CONTEXT_ROOT"
}

configure_java_memory() {
  export JAVA_TOOL_OPTIONS="-Xms$JAVA_INITIAL_HEAP -Xmx$JAVA_MAX_HEAP -Xss$JAVA_THREAD_STACK -XX:ActiveProcessorCount=$JAVA_ACTIVE_PROCESSORS -XX:CompressedClassSpaceSize=$JAVA_COMPRESSED_CLASS_SPACE -XX:MaxMetaspaceSize=$JAVA_MAX_METASPACE"
  export SOA_JAVA_MEMORY_CONFIGURED=1
}
