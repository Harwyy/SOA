param(
    [string]$PayaraHome = $env:PAYARA_HOME,
    [string]$Domain = "domain1"
)

$ErrorActionPreference = "Stop"
if ([string]::IsNullOrWhiteSpace($PayaraHome)) {
    throw "Укажите PAYARA_HOME или передайте -PayaraHome."
}

$asadmin = Join-Path $PayaraHome "bin\asadmin.bat"

& $asadmin set "server-config.network-config.network-listeners.network-listener.http-listener-1.port=8080"
& $asadmin set "server-config.network-config.network-listeners.network-listener.http-listener-1.enabled=true"
& $asadmin set "server-config.network-config.protocols.protocol.http-listener-1.security-enabled=false"
& $asadmin set "server-config.network-config.network-listeners.network-listener.http-listener-2.enabled=false"
& $asadmin restart-domain $Domain

Write-Host "Payara API: http://localhost:8080/city-service/cities"
Write-Host "TLS listener is not configured; admin listener remains available for administration."
