#!/usr/bin/env bash
# Configures WildFly for HTTPS-only local deployment of route-service.
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=config.sh
source "$SCRIPT_DIR/config.sh"

usage() {
  cat <<'EOF'
Usage: bash configure-wildfly.sh [options]

Options:
  --wildfly-home PATH    Overrides WILDFLY_HOME from config.sh
  --payara-home PATH     Overrides PAYARA_HOME from config.sh
  --payara-domain NAME   Overrides PAYARA_DOMAIN from config.sh
  --store-password TEXT  Overrides STORE_PASSWORD from config.sh
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --wildfly-home) WILDFLY_HOME="$2"; shift 2 ;;
    --payara-home) PAYARA_HOME="$2"; shift 2 ;;
    --payara-domain) PAYARA_DOMAIN="$2"; shift 2 ;;
    --store-password) STORE_PASSWORD="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage >&2; exit 2 ;;
  esac
done

refresh_soa_config
configure_java_memory

CLI="$WILDFLY_HOME/bin/jboss-cli.sh"
STANDALONE="$WILDFLY_HOME/bin/standalone.sh"
if [[ ! -x "$CLI" || ! -x "$STANDALONE" ]]; then
  echo "WildFly scripts were not found or are not executable in: $WILDFLY_HOME/bin" >&2
  exit 1
fi

if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/keytool" ]]; then
  KEYTOOL="$JAVA_HOME/bin/keytool"
else
  KEYTOOL="$(command -v keytool || true)"
fi
if [[ -z "$KEYTOOL" ]]; then
  echo "keytool was not found. Set JAVA_HOME or add keytool to PATH." >&2
  exit 1
fi

for thread_limit in "$WILDFLY_JCA_CORE_THREADS" "$WILDFLY_JCA_MAX_THREADS" "$WILDFLY_JCA_QUEUE_LENGTH"; do
  if [[ ! "$thread_limit" =~ ^[1-9][0-9]*$ ]]; then
    echo 'WildFly JCA thread limits in config.sh must be positive integers.' >&2
    exit 1
  fi
done

configure_jca_thread_limits() {
  local standalone_config="$WILDFLY_CONFIG_DIR/standalone.xml"
  if [[ ! -f "$standalone_config" ]]; then
    echo "WildFly configuration was not found: $standalone_config" >&2
    exit 1
  fi

  # The default (50 + 50) JCA worker pools exhaust a small per-user native
  # thread quota before WildFly can finish starting. This is applied to the
  # persisted configuration before the first boot and is idempotent.
  local temporary_config
  temporary_config="$(mktemp "$WILDFLY_CONFIG_DIR/standalone.xml.XXXXXX")"
  awk \
    -v core_threads="$WILDFLY_JCA_CORE_THREADS" \
    -v max_threads="$WILDFLY_JCA_MAX_THREADS" \
    -v queue_length="$WILDFLY_JCA_QUEUE_LENGTH" \
    '
      /<short-running-threads>|<long-running-threads>/ { in_jca_pool = 1 }
      in_jca_pool && /<core-threads / { sub(/count="[0-9]+"/, "count=\"" core_threads "\"") }
      in_jca_pool && /<queue-length / { sub(/count="[0-9]+"/, "count=\"" queue_length "\"") }
      in_jca_pool && /<max-threads / { sub(/count="[0-9]+"/, "count=\"" max_threads "\"") }
      { print }
      /<\/short-running-threads>|<\/long-running-threads>/ { in_jca_pool = 0 }
    ' "$standalone_config" >"$temporary_config"
  mv "$temporary_config" "$standalone_config"
}

configure_wildfly_jvm_memory() {
  local standalone_conf="$WILDFLY_HOME/bin/standalone.conf"
  local temporary_conf
  if [[ ! -f "$standalone_conf" ]]; then
    echo "WildFly startup configuration was not found: $standalone_conf" >&2
    exit 1
  fi

  # The stock startup file supplies -Xmx512m after JAVA_TOOL_OPTIONS, so it
  # would otherwise override the central memory limit.
  temporary_conf="$(mktemp "$WILDFLY_HOME/bin/standalone.conf.XXXXXX")"
  awk -v initial_heap="-Xms$JAVA_INITIAL_HEAP" -v max_heap="-Xmx$JAVA_MAX_HEAP" \
    '{ gsub(/-Xms[0-9]+[mMgG]/, initial_heap); gsub(/-Xmx[0-9]+[mMgG]/, max_heap); print }' \
    "$standalone_conf" >"$temporary_conf"
  mv "$temporary_conf" "$standalone_conf"
}

configure_wildfly_jvm_memory
configure_jca_thread_limits

if [[ -z "$CITY_CERTIFICATE_FILE" && ! -f "$PAYARA_KEYSTORE" ]]; then
  echo "Payara keystore was not found: $PAYARA_KEYSTORE" >&2
  echo 'Run configure-payara.sh first, or set CITY_CERTIFICATE_FILE to City service public certificate.' >&2
  exit 1
fi

route_certificate_matches_host() {
  [[ -f "$ROUTE_KEYSTORE" ]] \
    && "$KEYTOOL" -list -v -alias "$ROUTE_CERTIFICATE_ALIAS" -keystore "$ROUTE_KEYSTORE" \
      -storepass "$STORE_PASSWORD" 2>/dev/null | grep -Fq "DNSName: $CERTIFICATE_DNS_NAME"
}

# Regenerate when the service is moved to another public host. A certificate
# that only contains localhost is rejected by browsers at the public URL.
if ! route_certificate_matches_host; then
  rm -f "$ROUTE_KEYSTORE"
  "$KEYTOOL" -genkeypair -alias "$ROUTE_CERTIFICATE_ALIAS" -keyalg RSA -keysize 2048 -validity 3650 \
    -dname 'CN=localhost, OU=SOA, O=ITMO, L=Saint Petersburg, C=RU' \
    -ext "SAN=dns:$CERTIFICATE_DNS_NAME,ip:$CERTIFICATE_IP_ADDRESS" \
    -storetype PKCS12 -keystore "$ROUTE_KEYSTORE" \
    -storepass "$STORE_PASSWORD" -keypass "$STORE_PASSWORD"
fi

if [[ -n "$CITY_CERTIFICATE_FILE" ]]; then
  if [[ ! -f "$CITY_CERTIFICATE_FILE" ]]; then
    echo "City service certificate was not found: $CITY_CERTIFICATE_FILE" >&2
    exit 1
  fi
  TEMP_CERT="$CITY_CERTIFICATE_FILE"
else
  TEMP_CERT="$(mktemp "$WILDFLY_CONFIG_DIR/city-service-payara.XXXXXX")"
  trap 'rm -f "$TEMP_CERT"' EXIT
  "$KEYTOOL" -exportcert -rfc -alias "$CITY_CERTIFICATE_ALIAS" -keystore "$PAYARA_KEYSTORE" \
    -storepass "$STORE_PASSWORD" -file "$TEMP_CERT"
fi
rm -f "$CITY_TRUSTSTORE"
"$KEYTOOL" -importcert -noprompt -alias "$CITY_CERTIFICATE_ALIAS" -file "$TEMP_CERT" \
  -storetype PKCS12 -keystore "$CITY_TRUSTSTORE" -storepass "$STORE_PASSWORD"

is_management_available() {
  "$CLI" --connect --command=':read-attribute(name=server-state)' 2>/dev/null | grep -q '"outcome" => "success"'
}

is_running() {
  "$CLI" --connect --command=':read-attribute(name=server-state)' 2>/dev/null | grep -q '"result" => "running"'
}

wait_for_running() {
  for _ in {1..60}; do
    is_running && return 0
    sleep 1
  done
  echo "WildFly did not reach the running state within 60 seconds. Check $WILDFLY_LOG_DIR/soa-startup.log" >&2
  return 1
}

if ! is_management_available; then
  mkdir -p "$WILDFLY_LOG_DIR"
  nohup "$STANDALONE" -c standalone.xml -b "$WILDFLY_BIND_ADDRESS" \
    >"$WILDFLY_LOG_DIR/soa-startup.log" 2>&1 &
  for _ in {1..60}; do
    sleep 1
    is_management_available && break
  done
fi
if ! is_management_available; then
  echo "WildFly did not start within 30 seconds. Check $WILDFLY_LOG_DIR/soa-startup.log" >&2
  exit 1
fi

if ! is_running; then
  "$CLI" --connect --command=':reload' >/dev/null
  wait_for_running
fi

run_cli() {
  local output
  if ! output=$("$CLI" --connect "--command=$1" 2>&1) || ! grep -q '"outcome" => "success"' <<<"$output"; then
    echo "WildFly operation failed: $1" >&2
    echo "$output" >&2
    exit 1
  fi
}

resource_exists() {
  "$CLI" --connect "--command=$1:read-resource" 2>/dev/null | grep -q '"outcome" => "success"'
}

ensure_resource() {
  local address="$1"
  local operation="$2"
  if ! resource_exists "$address"; then
    run_cli "$address:add($operation)"
  fi
}

set_system_property() {
  local name="$1"
  local value="$2"
  local address="/system-property=$name"
  if resource_exists "$address"; then
    run_cli "$address:write-attribute(name=value,value=\"$value\")"
  else
    run_cli "$address:add(value=\"$value\")"
  fi
}

ensure_resource '/subsystem=elytron/key-store=routeServiceKS' \
  "path=$ROUTE_KEYSTORE_FILE,relative-to=jboss.server.config.dir,type=PKCS12,credential-reference={clear-text=\"$STORE_PASSWORD\"}"
ensure_resource '/subsystem=elytron/key-manager=routeServiceKM' \
  "key-store=routeServiceKS,credential-reference={clear-text=\"$STORE_PASSWORD\"}"
ensure_resource '/subsystem=elytron/server-ssl-context=routeServiceSSC' \
  'key-manager=routeServiceKM,protocols=[TLSv1.2,TLSv1.3]'
run_cli '/subsystem=undertow/server=default-server/https-listener=https:write-attribute(name=ssl-context,value=routeServiceSSC)'
run_cli "/socket-binding-group=standard-sockets/socket-binding=https:write-attribute(name=port,value=$ROUTE_SERVICE_PORT)"

if resource_exists '/subsystem=undertow/server=default-server/http-listener=default'; then
  run_cli '/subsystem=undertow/server=default-server/http-listener=default:remove'
fi
if resource_exists '/subsystem=ejb3/service=remote'; then
  run_cli '/subsystem=ejb3/service=remote:remove'
fi
if resource_exists '/subsystem=remoting/http-connector=http-remoting-connector'; then
  run_cli '/subsystem=remoting/http-connector=http-remoting-connector:remove'
fi

set_system_property 'city.service.base-url' "$CITY_SERVICE_BASE_URL"
set_system_property 'javax.net.ssl.trustStore' "$CITY_TRUSTSTORE"
set_system_property 'javax.net.ssl.trustStorePassword' "$STORE_PASSWORD"
set_system_property 'javax.net.ssl.trustStoreType' 'PKCS12'
run_cli ':reload'
wait_for_running

echo "WildFly configured: HTTPS is available at $ROUTE_SERVICE_BASE_URL; HTTP is disabled."
