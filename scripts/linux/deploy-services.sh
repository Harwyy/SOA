#!/usr/bin/env bash
# Deploys pre-built WAR files. It deliberately does not compile the project.
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=config.sh
source "$SCRIPT_DIR/config.sh"

usage() {
  cat <<'EOF'
Usage: bash deploy-services.sh [options]

Options:
  --payara-home PATH    Overrides PAYARA_HOME from config.sh
  --wildfly-home PATH   Overrides WILDFLY_HOME from config.sh
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --payara-home) PAYARA_HOME="$2"; shift 2 ;;
    --wildfly-home) WILDFLY_HOME="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage >&2; exit 2 ;;
  esac
done

refresh_soa_config
configure_java_memory

PROJECT_ROOT="$(cd -- "$SCRIPT_DIR/../.." && pwd)"
ASADMIN="$PAYARA_HOME/bin/asadmin"
CLI="$WILDFLY_HOME/bin/jboss-cli.sh"
CITY_WAR="$PROJECT_ROOT/$CITY_WAR_RELATIVE_PATH"
ROUTE_WAR="$PROJECT_ROOT/$ROUTE_WAR_RELATIVE_PATH"

for file in "$ASADMIN" "$CLI" "$CITY_WAR" "$ROUTE_WAR"; do
  if [[ ! -e "$file" ]]; then
    echo "Required file was not found: $file" >&2
    exit 1
  fi
done

"$ASADMIN" undeploy "$CITY_SERVICE_CONTEXT_ROOT" >/dev/null 2>&1 || true
"$ASADMIN" deploy --name "$CITY_SERVICE_CONTEXT_ROOT" --contextroot "$CITY_SERVICE_CONTEXT_ROOT" "$CITY_WAR"

"$CLI" --connect --command="undeploy $ROUTE_SERVICE_DEPLOYMENT_NAME" >/dev/null 2>&1 || true
"$CLI" --connect --command="deploy \"$ROUTE_WAR\" --name=$ROUTE_SERVICE_DEPLOYMENT_NAME"

echo 'Both pre-built WAR archives were deployed.'
