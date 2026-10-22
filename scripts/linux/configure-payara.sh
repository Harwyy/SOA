#!/usr/bin/env bash
# Configures the Payara domain used by city-service for HTTPS-only local deployment.
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=config.sh
source "$SCRIPT_DIR/config.sh"

usage() {
  cat <<'EOF'
Usage: bash configure-payara.sh [options]

Options:
  --payara-home PATH     Overrides PAYARA_HOME from config.sh
  --domain NAME          Overrides PAYARA_DOMAIN from config.sh
  --store-password TEXT  Overrides STORE_PASSWORD from config.sh
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --payara-home) PAYARA_HOME="$2"; shift 2 ;;
    --domain) PAYARA_DOMAIN="$2"; shift 2 ;;
    --store-password) STORE_PASSWORD="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage >&2; exit 2 ;;
  esac
done

refresh_soa_config
configure_java_memory

ASADMIN="$PAYARA_HOME/bin/asadmin"
if [[ ! -x "$ASADMIN" ]]; then
  echo "Payara asadmin was not found or is not executable: $ASADMIN" >&2
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

if [[ ! -d "$PAYARA_CONFIG_DIR" ]]; then
  echo "Payara domain configuration was not found: $PAYARA_CONFIG_DIR" >&2
  exit 1
fi

configure_domain_jvm_memory() {
  local domain_xml="$PAYARA_CONFIG_DIR/domain.xml"
  local temporary_config
  if [[ ! -f "$domain_xml" ]]; then
    echo "Payara domain descriptor was not found: $domain_xml" >&2
    exit 1
  fi

  # Payara's stock domain.xml contains -Xmx512m, which takes precedence over
  # JAVA_TOOL_OPTIONS. Persist the configured limit before the first start.
  temporary_config="$(mktemp "$PAYARA_CONFIG_DIR/domain.xml.XXXXXX")"
  awk -v heap="-Xmx$JAVA_MAX_HEAP" -v admin_port="$PAYARA_ADMIN_PORT" \
    '{
       gsub(/<jvm-options>-Xmx[0-9]+[mMgG]<\/jvm-options>/, "<jvm-options>" heap "</jvm-options>");
       gsub(/port="[0-9]+" name="admin-listener"/, "port=\"" admin_port "\" name=\"admin-listener\"");
       print
     }' \
    "$domain_xml" >"$temporary_config"
  mv "$temporary_config" "$domain_xml"
}

is_admin_port_open() {
  timeout 1 bash -c "</dev/tcp/127.0.0.1/$PAYARA_ADMIN_PORT" 2>/dev/null
}

wait_for_payara_stop() {
  for _ in {1..60}; do
    if ! is_admin_port_open; then
      return 0
    fi
    sleep 1
  done
  echo "Payara did not stop within 60 seconds." >&2
  return 1
}

# The keystore must not be modified while Payara is using it.
"$ASADMIN" --port "$PAYARA_ADMIN_PORT" stop-domain "$PAYARA_DOMAIN" >/dev/null 2>&1 || true
wait_for_payara_stop
configure_domain_jvm_memory

if ! "$KEYTOOL" -list -alias "$CITY_CERTIFICATE_ALIAS" -keystore "$PAYARA_KEYSTORE" \
    -storepass "$STORE_PASSWORD" >/dev/null 2>&1; then
  "$KEYTOOL" -genkeypair -alias "$CITY_CERTIFICATE_ALIAS" -keyalg RSA -keysize 2048 -validity 3650 \
    -dname 'CN=localhost, OU=SOA, O=ITMO, L=Saint Petersburg, C=RU' \
    -ext "SAN=dns:$CERTIFICATE_DNS_NAME,ip:$CERTIFICATE_IP_ADDRESS" \
    -storetype PKCS12 -keystore "$PAYARA_KEYSTORE" \
    -storepass "$STORE_PASSWORD" -keypass "$STORE_PASSWORD"
fi

start_payara() {
  if is_admin_port_open; then
    return 0
  fi
  mkdir -p "$PAYARA_LOG_DIR"
  nohup "$ASADMIN" start-domain --verbose "$PAYARA_DOMAIN" \
    >"$PAYARA_LOG_DIR/soa-startup.log" 2>&1 &
  for _ in {1..60}; do
    if is_admin_port_open; then
      return 0
    fi
    sleep 1
  done
  echo "Payara did not open its admin port within 60 seconds. Check $PAYARA_LOG_DIR/soa-startup.log" >&2
  return 1
}

start_payara

set_payara_value() {
  "$ASADMIN" --port "$PAYARA_ADMIN_PORT" set "$1=$2" >/dev/null
}

# Disable unencrypted HTTP and serve the application via the default HTTPS listener.
set_payara_value 'configs.config.server-config.network-config.network-listeners.network-listener.http-listener-1.enabled' 'false'
set_payara_value 'configs.config.server-config.network-config.network-listeners.network-listener.http-listener-2.enabled' 'true'
set_payara_value 'configs.config.server-config.network-config.network-listeners.network-listener.http-listener-2.port' "$CITY_SERVICE_PORT"
set_payara_value 'configs.config.server-config.network-config.protocols.protocol.http-listener-2.security-enabled' 'true'
set_payara_value 'configs.config.server-config.network-config.protocols.protocol.http-listener-2.ssl.cert-nickname' "$CITY_CERTIFICATE_ALIAS"

# Keep all Payara worker pools small enough to coexist with WildFly on a
# shared account with a low native-thread limit.
set_payara_value 'configs.config.server-config.thread-pools.thread-pool.http-thread-pool.min-thread-pool-size' '1'
set_payara_value 'configs.config.server-config.thread-pools.thread-pool.http-thread-pool.max-thread-pool-size' "$PAYARA_HTTP_MAX_THREADS"
set_payara_value 'configs.config.server-config.thread-pools.thread-pool.http-thread-pool.max-queue-size' "$PAYARA_THREAD_QUEUE_SIZE"
set_payara_value 'configs.config.server-config.thread-pools.thread-pool.admin-thread-pool.min-thread-pool-size' '1'
set_payara_value 'configs.config.server-config.thread-pools.thread-pool.admin-thread-pool.max-thread-pool-size' "$PAYARA_ADMIN_MAX_THREADS"
set_payara_value 'configs.config.server-config.thread-pools.thread-pool.admin-thread-pool.max-queue-size' "$PAYARA_THREAD_QUEUE_SIZE"
set_payara_value 'configs.config.server-config.thread-pools.thread-pool.thread-pool-1.min-thread-pool-size' '1'
set_payara_value 'configs.config.server-config.thread-pools.thread-pool.thread-pool-1.max-thread-pool-size' "$PAYARA_ORB_MAX_THREADS"
set_payara_value 'configs.config.server-config.thread-pools.thread-pool.thread-pool-1.max-queue-size' "$PAYARA_THREAD_QUEUE_SIZE"

"$ASADMIN" --port "$PAYARA_ADMIN_PORT" stop-domain "$PAYARA_DOMAIN" >/dev/null 2>&1 || true
wait_for_payara_stop
start_payara
echo "Payara configured: HTTPS is available at $CITY_SERVICE_BASE_URL; HTTP is disabled."
