#!/usr/bin/env bash
# Configures Payara HTTPS for City service.
set -e

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/config.sh"
refresh_soa_config
configure_java_memory

ASADMIN="$PAYARA_HOME/bin/asadmin"
KEYTOOL=keytool

awk -v heap="-Xmx$JAVA_MAX_HEAP" -v admin_port="$PAYARA_ADMIN_PORT" '
  { gsub(/<jvm-options>-Xmx[0-9]+[mMgG]<\/jvm-options>/, "<jvm-options>" heap "<\/jvm-options>") }
  { gsub(/port="[0-9]+" name="admin-listener"/, "port=\"" admin_port "\" name=\"admin-listener\"") }
  { print }
' "$PAYARA_CONFIG_DIR/domain.xml" >"$PAYARA_CONFIG_DIR/domain.xml.new"
mv "$PAYARA_CONFIG_DIR/domain.xml.new" "$PAYARA_CONFIG_DIR/domain.xml"

"$ASADMIN" --port "$PAYARA_ADMIN_PORT" stop-domain "$PAYARA_DOMAIN" || true
sleep 10
"$KEYTOOL" -genkeypair -alias "$CITY_CERTIFICATE_ALIAS" -keyalg RSA -keysize 2048 -validity 3650 \
  -dname 'CN=localhost, OU=SOA, O=ITMO, L=Saint Petersburg, C=RU' \
  -ext "SAN=dns:$CERTIFICATE_DNS_NAME,ip:$CERTIFICATE_IP_ADDRESS" \
  -storetype PKCS12 -keystore "$PAYARA_KEYSTORE" -storepass "$STORE_PASSWORD" -keypass "$STORE_PASSWORD" || true
nohup "$ASADMIN" start-domain --verbose "$PAYARA_DOMAIN" >"$PAYARA_LOG_DIR/soa-startup.log" 2>&1 &
sleep 20

set_property() {
  value="${2//\\/\\\\}"
  value="${value//:/\\:}"
  value="${value//=/\\=}"
  "$ASADMIN" --port "$PAYARA_ADMIN_PORT" delete-system-property --target server "$1" || true
  "$ASADMIN" --port "$PAYARA_ADMIN_PORT" create-system-properties --target server "$1=$value"
}

set_property 'city.db.url' "$CITY_DB_URL"
set_property 'city.db.user' "$CITY_DB_USER"
set_property 'city.db.password' "$CITY_DB_PASSWORD"
set_property 'city.db.schema-mode' "$CITY_DB_SCHEMA_MODE"

"$ASADMIN" --port "$PAYARA_ADMIN_PORT" set 'configs.config.server-config.network-config.network-listeners.network-listener.http-listener-1.enabled=false'
"$ASADMIN" --port "$PAYARA_ADMIN_PORT" set 'configs.config.server-config.network-config.network-listeners.network-listener.http-listener-2.enabled=true'
"$ASADMIN" --port "$PAYARA_ADMIN_PORT" set "configs.config.server-config.network-config.network-listeners.network-listener.http-listener-2.port=$CITY_SERVICE_PORT"
"$ASADMIN" --port "$PAYARA_ADMIN_PORT" set 'configs.config.server-config.network-config.protocols.protocol.http-listener-2.security-enabled=true'
"$ASADMIN" --port "$PAYARA_ADMIN_PORT" set "configs.config.server-config.network-config.protocols.protocol.http-listener-2.ssl.cert-nickname=$CITY_CERTIFICATE_ALIAS"
"$ASADMIN" --port "$PAYARA_ADMIN_PORT" set 'configs.config.server-config.thread-pools.thread-pool.http-thread-pool.min-thread-pool-size=1'
"$ASADMIN" --port "$PAYARA_ADMIN_PORT" set "configs.config.server-config.thread-pools.thread-pool.http-thread-pool.max-thread-pool-size=$PAYARA_HTTP_MAX_THREADS"
"$ASADMIN" --port "$PAYARA_ADMIN_PORT" set "configs.config.server-config.thread-pools.thread-pool.http-thread-pool.max-queue-size=$PAYARA_THREAD_QUEUE_SIZE"
"$ASADMIN" --port "$PAYARA_ADMIN_PORT" set 'configs.config.server-config.thread-pools.thread-pool.admin-thread-pool.min-thread-pool-size=1'
"$ASADMIN" --port "$PAYARA_ADMIN_PORT" set "configs.config.server-config.thread-pools.thread-pool.admin-thread-pool.max-thread-pool-size=$PAYARA_ADMIN_MAX_THREADS"
"$ASADMIN" --port "$PAYARA_ADMIN_PORT" set "configs.config.server-config.thread-pools.thread-pool.admin-thread-pool.max-queue-size=$PAYARA_THREAD_QUEUE_SIZE"
"$ASADMIN" --port "$PAYARA_ADMIN_PORT" set 'configs.config.server-config.thread-pools.thread-pool.thread-pool-1.min-thread-pool-size=1'
"$ASADMIN" --port "$PAYARA_ADMIN_PORT" set "configs.config.server-config.thread-pools.thread-pool.thread-pool-1.max-thread-pool-size=$PAYARA_ORB_MAX_THREADS"
"$ASADMIN" --port "$PAYARA_ADMIN_PORT" set "configs.config.server-config.thread-pools.thread-pool.thread-pool-1.max-queue-size=$PAYARA_THREAD_QUEUE_SIZE"

"$ASADMIN" --port "$PAYARA_ADMIN_PORT" stop-domain "$PAYARA_DOMAIN"
sleep 10
nohup "$ASADMIN" start-domain --verbose "$PAYARA_DOMAIN" >"$PAYARA_LOG_DIR/soa-startup.log" 2>&1 &
sleep 20
