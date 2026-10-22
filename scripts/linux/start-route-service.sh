#!/usr/bin/env bash
# Configures WildFly and deploys an already-built route-service WAR.
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=config.sh
source "$SCRIPT_DIR/config.sh"
refresh_soa_config
configure_java_memory

CLI="$WILDFLY_HOME/bin/jboss-cli.sh"
PROJECT_ROOT="$(cd -- "$SCRIPT_DIR/../.." && pwd)"
ROUTE_WAR="$PROJECT_ROOT/$ROUTE_WAR_RELATIVE_PATH"
for file in "$CLI" "$ROUTE_WAR"; do
  [[ -e "$file" ]] || { echo "Required file was not found: $file" >&2; exit 1; }
done

bash "$SCRIPT_DIR/configure-wildfly.sh"
"$CLI" --connect --command="undeploy $ROUTE_SERVICE_DEPLOYMENT_NAME" >/dev/null 2>&1 || true
"$CLI" --connect --command=':reload' >/dev/null
for _ in {1..60}; do
  if "$CLI" --connect --command=':read-attribute(name=server-state)' 2>/dev/null | grep -q '"result" => "running"'; then
    break
  fi
  sleep 1
done
if ! "$CLI" --connect --command=':read-attribute(name=server-state)' 2>/dev/null | grep -q '"result" => "running"'; then
  echo 'WildFly did not return to the running state after redeployment cleanup.' >&2
  exit 1
fi
"$CLI" --connect --command="deploy \"$ROUTE_WAR\" --name=$ROUTE_SERVICE_DEPLOYMENT_NAME"
echo "Route service is running at $ROUTE_SERVICE_BASE_URL/route-service"
