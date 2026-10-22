#!/usr/bin/env bash
# Adds local Payara and WildFly self-signed certificates to the Linux system CA store.
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=config.sh
source "$SCRIPT_DIR/config.sh"

usage() {
  cat <<'EOF'
Usage: bash trust-local-certificates.sh [options]

This command requires sudo. It supports Debian/Ubuntu (update-ca-certificates)
and RHEL/Fedora/AlmaLinux/Rocky Linux (update-ca-trust).

Options:
  --payara-home PATH     Overrides PAYARA_HOME from config.sh
  --wildfly-home PATH    Overrides WILDFLY_HOME from config.sh
  --payara-domain NAME   Overrides PAYARA_DOMAIN from config.sh
  --store-password TEXT  Overrides STORE_PASSWORD from config.sh
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --payara-home) PAYARA_HOME="$2"; shift 2 ;;
    --wildfly-home) WILDFLY_HOME="$2"; shift 2 ;;
    --payara-domain) PAYARA_DOMAIN="$2"; shift 2 ;;
    --store-password) STORE_PASSWORD="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage >&2; exit 2 ;;
  esac
done

refresh_soa_config
configure_java_memory

if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/keytool" ]]; then
  KEYTOOL="$JAVA_HOME/bin/keytool"
else
  KEYTOOL="$(command -v keytool || true)"
fi
if [[ -z "$KEYTOOL" ]]; then
  echo "keytool was not found. Set JAVA_HOME or add keytool to PATH." >&2
  exit 1
fi

for store in "$PAYARA_KEYSTORE" "$ROUTE_KEYSTORE"; do
  if [[ ! -f "$store" ]]; then
    echo "Keystore was not found: $store" >&2
    echo 'Configure both application servers before trusting their certificates.' >&2
    exit 1
  fi
done

TEMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/soa-local-certificates.XXXXXX")"
trap 'rm -rf "$TEMP_DIR"' EXIT

"$KEYTOOL" -exportcert -rfc -alias "$CITY_CERTIFICATE_ALIAS" -keystore "$PAYARA_KEYSTORE" \
  -storepass "$STORE_PASSWORD" -file "$TEMP_DIR/city-service.crt"
"$KEYTOOL" -exportcert -rfc -alias "$ROUTE_CERTIFICATE_ALIAS" -keystore "$ROUTE_KEYSTORE" \
  -storepass "$STORE_PASSWORD" -file "$TEMP_DIR/route-service.crt"

if command -v update-ca-certificates >/dev/null; then
  sudo install -Dm644 "$TEMP_DIR/city-service.crt" /usr/local/share/ca-certificates/soa/city-service.crt
  sudo install -Dm644 "$TEMP_DIR/route-service.crt" /usr/local/share/ca-certificates/soa/route-service.crt
  sudo update-ca-certificates
elif command -v update-ca-trust >/dev/null; then
  sudo install -Dm644 "$TEMP_DIR/city-service.crt" /etc/pki/ca-trust/source/anchors/city-service.crt
  sudo install -Dm644 "$TEMP_DIR/route-service.crt" /etc/pki/ca-trust/source/anchors/route-service.crt
  sudo update-ca-trust extract
else
  echo 'No supported system CA-store tool was found (update-ca-certificates or update-ca-trust).' >&2
  exit 1
fi

echo 'The local HTTPS certificates are trusted by the Linux system CA store.'
