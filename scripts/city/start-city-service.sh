#!/usr/bin/env bash
# Configures Payara and deploys City service.
set -e

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/config.sh"
refresh_soa_config
configure_java_memory

bash "$SCRIPT_DIR/configure-payara.sh"

ASADMIN="$PAYARA_HOME/bin/asadmin"
CITY_WAR="$(cd "$SCRIPT_DIR/../.." && pwd)/$CITY_WAR_RELATIVE_PATH"
"$ASADMIN" --port "$PAYARA_ADMIN_PORT" undeploy "$CITY_SERVICE_CONTEXT_ROOT" || true
sleep 20
"$ASADMIN" --port "$PAYARA_ADMIN_PORT" deploy --name "$CITY_SERVICE_CONTEXT_ROOT" --contextroot "$CITY_SERVICE_CONTEXT_ROOT" "$CITY_WAR"
curl -k -i 'https://127.0.0.1:18081/city-service/cities?page=1&size=10'
