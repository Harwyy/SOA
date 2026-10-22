#!/usr/bin/env bash
# Configures Payara and deploys an already-built city-service WAR.
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=config.sh
source "$SCRIPT_DIR/config.sh"
refresh_soa_config
configure_java_memory

ASADMIN="$PAYARA_HOME/bin/asadmin"
if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/keytool" ]]; then
  KEYTOOL="$JAVA_HOME/bin/keytool"
else
  KEYTOOL="$(command -v keytool || true)"
fi
PROJECT_ROOT="$(cd -- "$SCRIPT_DIR/../.." && pwd)"
CITY_WAR="$PROJECT_ROOT/$CITY_WAR_RELATIVE_PATH"

for file in "$ASADMIN" "$KEYTOOL" "$CITY_WAR"; do
  [[ -e "$file" ]] || { echo "Required file was not found: $file" >&2; exit 1; }
done

# Regenerate the certificate when its DNS SAN does not match this deployment.
if "$ASADMIN" --port "$PAYARA_ADMIN_PORT" stop-domain "$PAYARA_DOMAIN" >/dev/null 2>&1 || true; then :; fi
if "$KEYTOOL" -list -v -alias "$CITY_CERTIFICATE_ALIAS" -keystore "$PAYARA_KEYSTORE" \
  -storepass "$STORE_PASSWORD" 2>/dev/null | grep -q "DNSName: $CERTIFICATE_DNS_NAME"; then
  :
else
  "$KEYTOOL" -delete -alias "$CITY_CERTIFICATE_ALIAS" -keystore "$PAYARA_KEYSTORE" \
    -storepass "$STORE_PASSWORD" >/dev/null 2>&1 || true
fi

bash "$SCRIPT_DIR/configure-payara.sh"
if "$ASADMIN" --port "$PAYARA_ADMIN_PORT" list-applications 2>/dev/null | grep -q "^$CITY_SERVICE_CONTEXT_ROOT"; then
  "$ASADMIN" --port "$PAYARA_ADMIN_PORT" undeploy "$CITY_SERVICE_CONTEXT_ROOT"
  for _ in {1..30}; do
    if ! "$ASADMIN" --port "$PAYARA_ADMIN_PORT" list-applications 2>/dev/null | grep -q "^$CITY_SERVICE_CONTEXT_ROOT"; then
      break
    fi
    sleep 1
  done
fi
"$ASADMIN" --port "$PAYARA_ADMIN_PORT" deploy --name "$CITY_SERVICE_CONTEXT_ROOT" \
  --contextroot "$CITY_SERVICE_CONTEXT_ROOT" "$CITY_WAR"
echo "City service is running at $CITY_SERVICE_BASE_URL"
