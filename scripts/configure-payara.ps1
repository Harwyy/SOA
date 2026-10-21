param(
    [string]$PayaraHome = 'C:\Tools\payara6',
    [string]$Domain = 'domain1',
    [string]$StorePassword = 'changeit'
)

$ErrorActionPreference = 'Stop'
$asadmin = Join-Path $PayaraHome 'bin\asadmin.bat'
$keytool = Join-Path $env:JAVA_HOME 'bin\keytool.exe'
if (-not (Test-Path -LiteralPath $asadmin)) { throw "Payara was not found: $PayaraHome" }
if (-not (Test-Path -LiteralPath $keytool)) { $keytool = 'keytool' }

$configDir = Join-Path $PayaraHome "glassfish\domains\$Domain\config"
$keystore = Join-Path $configDir 'keystore.p12'
if (-not (Test-Path -LiteralPath $keystore)) { throw "Payara domain keystore was not found: $keystore" }

& $asadmin stop-domain $Domain 2>$null
if ($LASTEXITCODE -ne 0) { Write-Host 'Payara domain is already stopped.' }

& $keytool -list -alias city-service -keystore $keystore -storepass $StorePassword 2>$null
if ($LASTEXITCODE -ne 0) {
    & $keytool -genkeypair -alias city-service -keyalg RSA -keysize 2048 -validity 3650 `
        -dname 'CN=localhost, OU=SOA, O=ITMO, L=Saint Petersburg, C=RU' `
        -ext 'SAN=dns:localhost,ip:127.0.0.1' `
        -storetype PKCS12 -keystore $keystore -storepass $StorePassword -keypass $StorePassword
    if ($LASTEXITCODE -ne 0) { throw 'Could not create the Payara certificate.' }
}

& $asadmin start-domain $Domain
if ($LASTEXITCODE -ne 0) { throw 'Could not start the Payara domain.' }

function Set-PayaraValue([string]$name, [string]$value) {
    & $asadmin set "$name=$value"
    if ($LASTEXITCODE -ne 0) { throw "Could not set Payara property: $name" }
}

# Disable the unencrypted listener completely; applications use http-listener-2 only.
Set-PayaraValue 'configs.config.server-config.network-config.network-listeners.network-listener.http-listener-1.enabled' 'false'
Set-PayaraValue 'configs.config.server-config.network-config.network-listeners.network-listener.http-listener-2.enabled' 'true'
Set-PayaraValue 'configs.config.server-config.network-config.network-listeners.network-listener.http-listener-2.port' '8181'
Set-PayaraValue 'configs.config.server-config.network-config.protocols.protocol.http-listener-2.security-enabled' 'true'
Set-PayaraValue 'configs.config.server-config.network-config.protocols.protocol.http-listener-2.ssl.cert-nickname' 'city-service'

& $asadmin restart-domain $Domain
if ($LASTEXITCODE -ne 0) { throw 'Could not restart the Payara domain.' }
Write-Host 'Payara configured: HTTPS https://localhost:8181, HTTP listener disabled.'
