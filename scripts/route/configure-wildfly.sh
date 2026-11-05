#!/usr/bin/env bash
# Configures WildFly HTTPS for Route service.
set -e

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/config.sh"
refresh_soa_config
configure_java_memory

CLI="$WILDFLY_HOME/bin/jboss-cli.sh"
KEYTOOL=keytool

awk -v initial_heap="-Xms$JAVA_INITIAL_HEAP" -v max_heap="-Xmx$JAVA_MAX_HEAP" \
  '{ gsub(/-Xms[0-9]+[mMgG]/, initial_heap); gsub(/-Xmx[0-9]+[mMgG]/, max_heap); print }' \
  "$WILDFLY_HOME/bin/standalone.conf" >"$WILDFLY_HOME/bin/standalone.conf.new"
mv "$WILDFLY_HOME/bin/standalone.conf.new" "$WILDFLY_HOME/bin/standalone.conf"

awk -v threads="$WILDFLY_JCA_CORE_THREADS" -v queue="$WILDFLY_JCA_QUEUE_LENGTH" '
  /<short-running-threads>|<long-running-threads>/ { pool = 1 }
  pool && /<core-threads / { sub(/count="[0-9]+"/, "count=\"" threads "\"") }
  pool && /<queue-length / { sub(/count="[0-9]+"/, "count=\"" queue "\"") }
  pool && /<max-threads / { sub(/count="[0-9]+"/, "count=\"" threads "\"") }
  { print }
  /<\/short-running-threads>|<\/long-running-threads>/ { pool = 0 }
' "$WILDFLY_CONFIG_DIR/standalone.xml" >"$WILDFLY_CONFIG_DIR/standalone.xml.new"
mv "$WILDFLY_CONFIG_DIR/standalone.xml.new" "$WILDFLY_CONFIG_DIR/standalone.xml"

rm -f "$ROUTE_KEYSTORE" "$CITY_TRUSTSTORE"
"$KEYTOOL" -genkeypair -alias "$ROUTE_CERTIFICATE_ALIAS" -keyalg RSA -keysize 2048 -validity 3650 \
  -dname 'CN=localhost, OU=SOA, O=ITMO, L=Saint Petersburg, C=RU' \
  -ext "SAN=dns:$CERTIFICATE_DNS_NAME,ip:$CERTIFICATE_IP_ADDRESS" \
  -storetype PKCS12 -keystore "$ROUTE_KEYSTORE" -storepass "$STORE_PASSWORD" -keypass "$STORE_PASSWORD"
"$KEYTOOL" -importcert -noprompt -alias "$CITY_CERTIFICATE_ALIAS" -file "$CITY_CERTIFICATE_FILE" \
  -storetype PKCS12 -keystore "$CITY_TRUSTSTORE" -storepass "$STORE_PASSWORD"

nohup "$WILDFLY_HOME/bin/standalone.sh" -c standalone.xml -b "$WILDFLY_BIND_ADDRESS" \
  -Djboss.management.http.port="$WILDFLY_MANAGEMENT_PORT" \
  -Djboss.bind.address.management="$WILDFLY_MANAGEMENT_HOST" \
  >"$WILDFLY_LOG_DIR/soa-startup.log" 2>&1 &
sleep 20

cli() {
  "$CLI" --connect --controller="$WILDFLY_MANAGEMENT_HOST:$WILDFLY_MANAGEMENT_PORT" --command="$1"
}

cli "/subsystem=elytron/key-store=routeServiceKS:add(path=$ROUTE_KEYSTORE_FILE,relative-to=jboss.server.config.dir,type=PKCS12,credential-reference={clear-text=\"$STORE_PASSWORD\"})" || true
cli "/subsystem=elytron/key-manager=routeServiceKM:add(key-store=routeServiceKS,credential-reference={clear-text=\"$STORE_PASSWORD\"})" || true
cli '/subsystem=elytron/server-ssl-context=routeServiceSSC:add(key-manager=routeServiceKM,protocols=[TLSv1.2,TLSv1.3])' || true
cli '/subsystem=undertow/server=default-server/https-listener=https:write-attribute(name=ssl-context,value=routeServiceSSC)'
cli "/socket-binding-group=standard-sockets/socket-binding=https:write-attribute(name=port,value=$ROUTE_SERVICE_PORT)"
cli '/subsystem=undertow/server=default-server/http-listener=default:remove' || true
cli '/subsystem=ejb3/service=remote:remove' || true
cli '/subsystem=remoting/http-connector=http-remoting-connector:remove' || true
cli "/system-property=city.service.base-url:write-attribute(name=value,value=\"$CITY_SERVICE_BASE_URL\")" || cli "/system-property=city.service.base-url:add(value=\"$CITY_SERVICE_BASE_URL\")"
cli "/system-property=javax.net.ssl.trustStore:write-attribute(name=value,value=\"$CITY_TRUSTSTORE\")" || cli "/system-property=javax.net.ssl.trustStore:add(value=\"$CITY_TRUSTSTORE\")"
cli "/system-property=javax.net.ssl.trustStorePassword:write-attribute(name=value,value=\"$STORE_PASSWORD\")" || cli "/system-property=javax.net.ssl.trustStorePassword:add(value=\"$STORE_PASSWORD\")"
cli '/system-property=javax.net.ssl.trustStoreType:write-attribute(name=value,value="PKCS12")' || cli '/system-property=javax.net.ssl.trustStoreType:add(value="PKCS12")'
cli ':reload'
sleep 20
