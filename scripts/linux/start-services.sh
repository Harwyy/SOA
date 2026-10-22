#!/usr/bin/env bash
# Performs a first-time Linux setup and deploys both already-built WAR archives.
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=config.sh
source "$SCRIPT_DIR/config.sh"

if [[ $# -ne 0 ]]; then
  echo "Usage: bash start-services.sh" >&2
  echo "Edit config.sh first; it is the single source of paths, ports and limits." >&2
  exit 2
fi

refresh_soa_config

for file in "$PAYARA_HOME/bin/asadmin" "$WILDFLY_HOME/bin/standalone.sh"; do
  if [[ ! -x "$file" ]]; then
    echo "Required server script was not found or is not executable: $file" >&2
    exit 1
  fi
done

# A rerun starts from a known state. WildFly must be down while Payara is first
# configured, otherwise the combined native-thread demand can exceed the quota.
if [[ -x "$WILDFLY_HOME/bin/jboss-cli.sh" ]]; then
  JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:+$JAVA_TOOL_OPTIONS }-Xms$JAVA_INITIAL_HEAP -Xmx$JAVA_MAX_HEAP -Xss$JAVA_THREAD_STACK -XX:ActiveProcessorCount=$JAVA_ACTIVE_PROCESSORS -XX:CompressedClassSpaceSize=$JAVA_COMPRESSED_CLASS_SPACE -XX:MaxMetaspaceSize=$JAVA_MAX_METASPACE" \
    "$WILDFLY_HOME/bin/jboss-cli.sh" --connect --command=':shutdown' >/dev/null 2>&1 || true
fi

# Payara must briefly start first to create/configure its certificate. On hosts
# with a very small native-thread quota, start WildFly while Payara is stopped;
# then bring Payara back once WildFly is fully booted.
bash "$SCRIPT_DIR/configure-payara.sh"
JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:+$JAVA_TOOL_OPTIONS }-Xms$JAVA_INITIAL_HEAP -Xmx$JAVA_MAX_HEAP -Xss$JAVA_THREAD_STACK -XX:ActiveProcessorCount=$JAVA_ACTIVE_PROCESSORS -XX:CompressedClassSpaceSize=$JAVA_COMPRESSED_CLASS_SPACE -XX:MaxMetaspaceSize=$JAVA_MAX_METASPACE" \
  "$PAYARA_HOME/bin/asadmin" stop-domain "$PAYARA_DOMAIN" >/dev/null 2>&1 || true
bash "$SCRIPT_DIR/configure-wildfly.sh"
bash "$SCRIPT_DIR/configure-payara.sh"
bash "$SCRIPT_DIR/deploy-services.sh"

echo "Both services are running: $CITY_SERVICE_BASE_URL and $ROUTE_SERVICE_BASE_URL/route-service"
