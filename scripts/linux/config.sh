#!/usr/bin/env bash
#
# Central Linux deployment settings for the SOA project.
# Edit this file before deployment. Environment variables or command-line options
# passed to an individual script still take precedence over these default values.
#

# Application-server installation directories.
PAYARA_HOME="${PAYARA_HOME:-/opt/payara6}"
PAYARA_DOMAIN="${PAYARA_DOMAIN:-domain1}"
PAYARA_ADMIN_PORT="${PAYARA_ADMIN_PORT:-4848}"
WILDFLY_HOME="${WILDFLY_HOME:-/opt/wildfly-41.0.1}"

# Network settings. The default configuration exposes the services locally only.
WILDFLY_BIND_ADDRESS="${WILDFLY_BIND_ADDRESS:-127.0.0.1}"
CITY_SERVICE_HOST="${CITY_SERVICE_HOST:-localhost}"
CITY_SERVICE_PORT="${CITY_SERVICE_PORT:-8181}"
ROUTE_SERVICE_PORT="${ROUTE_SERVICE_PORT:-8443}"

# Application names, context roots, and paths to already-built artifacts.
CITY_SERVICE_CONTEXT_ROOT="${CITY_SERVICE_CONTEXT_ROOT:-city-service}"
ROUTE_SERVICE_DEPLOYMENT_NAME="${ROUTE_SERVICE_DEPLOYMENT_NAME:-route-service.war}"
CITY_WAR_RELATIVE_PATH="${CITY_WAR_RELATIVE_PATH:-city-service/target/city-service-1.0.0.war}"
ROUTE_WAR_RELATIVE_PATH="${ROUTE_WAR_RELATIVE_PATH:-route-service/target/route-service-1.0.0.war}"

# Paths inside the application-server installations.
PAYARA_KEYSTORE_FILE="${PAYARA_KEYSTORE_FILE:-keystore.p12}"
WILDFLY_CONFIGURATION_RELATIVE_PATH="${WILDFLY_CONFIGURATION_RELATIVE_PATH:-standalone/configuration}"
WILDFLY_LOG_RELATIVE_PATH="${WILDFLY_LOG_RELATIVE_PATH:-standalone/log}"
ROUTE_KEYSTORE_FILE="${ROUTE_KEYSTORE_FILE:-route-service.p12}"
CITY_TRUSTSTORE_FILE="${CITY_TRUSTSTORE_FILE:-city-service-truststore.p12}"
# Optional PEM/DER certificate of City service for deployments where Payara and
# WildFly run under different accounts or on different hosts.
CITY_CERTIFICATE_FILE="${CITY_CERTIFICATE_FILE:-}"

# HTTPS certificate and Java PKCS#12 storage settings.
STORE_PASSWORD="${STORE_PASSWORD:-changeit}"
CITY_CERTIFICATE_ALIAS="${CITY_CERTIFICATE_ALIAS:-city-service}"
ROUTE_CERTIFICATE_ALIAS="${ROUTE_CERTIFICATE_ALIAS:-route-service}"
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

# Payara thread-pool limits. Values are intentionally conservative so Payara
# can coexist with WildFly under a strict per-user native-thread quota.
PAYARA_HTTP_MAX_THREADS="${PAYARA_HTTP_MAX_THREADS:-4}"
PAYARA_ADMIN_MAX_THREADS="${PAYARA_ADMIN_MAX_THREADS:-2}"
PAYARA_ORB_MAX_THREADS="${PAYARA_ORB_MAX_THREADS:-4}"
PAYARA_THREAD_QUEUE_SIZE="${PAYARA_THREAD_QUEUE_SIZE:-32}"

# WildFly JCA work-manager thread limits. Keep these deliberately small on
# shared university hosts with a strict per-user native-thread quota.
WILDFLY_JCA_CORE_THREADS="${WILDFLY_JCA_CORE_THREADS:-1}"
WILDFLY_JCA_MAX_THREADS="${WILDFLY_JCA_MAX_THREADS:-1}"
WILDFLY_JCA_QUEUE_LENGTH="${WILDFLY_JCA_QUEUE_LENGTH:-1}"

# A server-specific override can live next to this file without changing the
# portable defaults committed to the project. Deployment scripts load this
# file automatically when it is present.
SOA_CONFIG_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
if [[ -f "$SOA_CONFIG_DIR/config.local.sh" ]]; then
  # shellcheck source=config.local.sh
  source "$SOA_CONFIG_DIR/config.local.sh"
fi
unset SOA_CONFIG_DIR

# Recalculate paths after a command-line override. All deployment scripts call it.
refresh_soa_config() {
  PAYARA_CONFIG_DIR="$PAYARA_HOME/glassfish/domains/$PAYARA_DOMAIN/config"
  PAYARA_LOG_DIR="$PAYARA_HOME/glassfish/domains/$PAYARA_DOMAIN/logs"
  PAYARA_KEYSTORE="$PAYARA_CONFIG_DIR/$PAYARA_KEYSTORE_FILE"
  WILDFLY_CONFIG_DIR="$WILDFLY_HOME/$WILDFLY_CONFIGURATION_RELATIVE_PATH"
  WILDFLY_LOG_DIR="$WILDFLY_HOME/$WILDFLY_LOG_RELATIVE_PATH"
  ROUTE_KEYSTORE="$WILDFLY_CONFIG_DIR/$ROUTE_KEYSTORE_FILE"
  CITY_TRUSTSTORE="$WILDFLY_CONFIG_DIR/$CITY_TRUSTSTORE_FILE"
  CITY_SERVICE_BASE_URL="https://$CITY_SERVICE_HOST:$CITY_SERVICE_PORT/$CITY_SERVICE_CONTEXT_ROOT"
  ROUTE_SERVICE_BASE_URL="https://$WILDFLY_BIND_ADDRESS:$ROUTE_SERVICE_PORT"
}

configure_java_memory() {
  if [[ "${SOA_JAVA_MEMORY_CONFIGURED:-}" == '1' ]]; then
    return
  fi
  export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:+$JAVA_TOOL_OPTIONS }-Xms$JAVA_INITIAL_HEAP -Xmx$JAVA_MAX_HEAP -Xss$JAVA_THREAD_STACK -XX:ActiveProcessorCount=$JAVA_ACTIVE_PROCESSORS -XX:CompressedClassSpaceSize=$JAVA_COMPRESSED_CLASS_SPACE -XX:MaxMetaspaceSize=$JAVA_MAX_METASPACE"
  export SOA_JAVA_MEMORY_CONFIGURED=1
}
