#!/usr/bin/env bash
# Configures WildFly and deploys Route service.
set -e

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/config.sh"
refresh_soa_config
configure_java_memory

bash "$SCRIPT_DIR/configure-wildfly.sh"

CLI="$WILDFLY_HOME/bin/jboss-cli.sh"
ROUTE_WAR="$(cd "$SCRIPT_DIR/../.." && pwd)/$ROUTE_WAR_RELATIVE_PATH"
"$CLI" --connect --controller="$WILDFLY_MANAGEMENT_HOST:$WILDFLY_MANAGEMENT_PORT" --command="undeploy $ROUTE_SERVICE_DEPLOYMENT_NAME" || true
"$CLI" --connect --controller="$WILDFLY_MANAGEMENT_HOST:$WILDFLY_MANAGEMENT_PORT" --command=':reload'
sleep 20
"$CLI" --connect --controller="$WILDFLY_MANAGEMENT_HOST:$WILDFLY_MANAGEMENT_PORT" --command="deploy \"$ROUTE_WAR\" --name=$ROUTE_SERVICE_DEPLOYMENT_NAME"
curl -k -i 'https://127.0.0.1:20000/route-service/route/calculate/to-oldest'
