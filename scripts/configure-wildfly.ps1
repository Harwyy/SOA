param(
    [string]$WildFlyHome = 'C:\Tools\wildfly-41.0.1',
    [string]$PayaraHome = 'C:\Tools\payara6',
    [string]$StorePassword = 'changeit'
)

$ErrorActionPreference = 'Stop'
$cli = Join-Path $WildFlyHome 'bin\jboss-cli.bat'
$standalone = Join-Path $WildFlyHome 'bin\standalone.bat'
$keytool = Join-Path $env:JAVA_HOME 'bin\keytool.exe'
if (-not (Test-Path -LiteralPath $cli)) { throw "WildFly was not found: $WildFlyHome" }
if (-not (Test-Path -LiteralPath $keytool)) { $keytool = 'keytool' }

$configDir = Join-Path $WildFlyHome 'standalone\configuration'
$routeKeystore = Join-Path $configDir 'route-service.p12'
$truststore = Join-Path $configDir 'city-service-truststore.p12'
$payaraKeystore = Join-Path $PayaraHome 'glassfish\domains\domain1\config\keystore.p12'
$payaraCertificate = Join-Path $env:TEMP 'city-service-payara.pem'

if (-not (Test-Path -LiteralPath $routeKeystore)) {
    & $keytool -genkeypair -alias route-service -keyalg RSA -keysize 2048 -validity 3650 `
        -dname 'CN=localhost, OU=SOA, O=ITMO, L=Saint Petersburg, C=RU' `
        -ext 'SAN=dns:localhost,ip:127.0.0.1' `
        -storetype PKCS12 -keystore $routeKeystore -storepass $StorePassword -keypass $StorePassword
    if ($LASTEXITCODE -ne 0) { throw 'Could not create the WildFly certificate.' }
}

if (-not (Test-Path -LiteralPath $payaraKeystore)) { throw "Run configure-payara.ps1 first: $payaraKeystore was not found." }
& $keytool -exportcert -rfc -alias city-service -keystore $payaraKeystore -storepass $StorePassword -file $payaraCertificate
if ($LASTEXITCODE -ne 0) { throw 'Could not export the Payara certificate.' }
if (Test-Path -LiteralPath $truststore) { Remove-Item -LiteralPath $truststore -Force }
& $keytool -importcert -noprompt -alias city-service -file $payaraCertificate -storetype PKCS12 -keystore $truststore -storepass $StorePassword
Remove-Item -LiteralPath $payaraCertificate -Force
if ($LASTEXITCODE -ne 0) { throw 'Could not create the WildFly truststore.' }

$server = Start-Process -FilePath $standalone -ArgumentList '-c', 'standalone.xml', '-b', '127.0.0.1' -WindowStyle Hidden -PassThru
for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 1
    $state = & $cli --connect --command=':read-attribute(name=server-state)' 2>$null
    if ($LASTEXITCODE -eq 0 -and $state -match 'running') { break }
    if ($i -eq 29) { throw 'WildFly did not start within 30 seconds.' }
}

function Invoke-Cli([string]$operation) {
    $result = & $cli --connect "--command=$operation"
    if ($LASTEXITCODE -ne 0 -or $result -match '"outcome" => "failed"') { throw "WildFly operation failed: $operation`n$result" }
}

Invoke-Cli '/subsystem=elytron/key-store=routeServiceKS:add(path=route-service.p12,relative-to=jboss.server.config.dir,type=PKCS12,credential-reference={clear-text="changeit"})'
Invoke-Cli '/subsystem=elytron/key-manager=routeServiceKM:add(key-store=routeServiceKS,credential-reference={clear-text="changeit"})'
Invoke-Cli '/subsystem=elytron/server-ssl-context=routeServiceSSC:add(key-manager=routeServiceKM,protocols=[TLSv1.2,TLSv1.3])'
Invoke-Cli '/subsystem=undertow/server=default-server/https-listener=https:write-attribute(name=ssl-context,value=routeServiceSSC)'
Invoke-Cli '/subsystem=undertow/server=default-server/http-listener=default:remove'
Invoke-Cli '/subsystem=ejb3/service=remote:remove'
Invoke-Cli '/subsystem=remoting/http-connector=http-remoting-connector:remove'
Invoke-Cli '/system-property=city.service.base-url:add(value=https://localhost:8181/city-service)'
Invoke-Cli "/system-property=javax.net.ssl.trustStore:add(value=$($truststore.Replace('\','/')))"
Invoke-Cli '/system-property=javax.net.ssl.trustStorePassword:add(value=changeit)'
Invoke-Cli '/system-property=javax.net.ssl.trustStoreType:add(value=PKCS12)'
Invoke-Cli ':reload'
Write-Host 'WildFly configured: HTTPS https://localhost:8443, HTTP listener removed.'
