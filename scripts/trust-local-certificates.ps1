param(
    [string]$PayaraHome = 'C:\Tools\payara6',
    [string]$WildFlyHome = 'C:\Tools\wildfly-41.0.1',
    [string]$StorePassword = 'changeit'
)

$ErrorActionPreference = 'Stop'
$keytool = Join-Path $env:JAVA_HOME 'bin\keytool.exe'
if (-not (Test-Path -LiteralPath $keytool)) { $keytool = 'keytool' }
$tempDirectory = Join-Path $env:TEMP 'soa-local-certificates'
New-Item -ItemType Directory -Path $tempDirectory -Force | Out-Null

$certificates = @(
    @{ Alias = 'city-service'; Store = Join-Path $PayaraHome 'glassfish\domains\domain1\config\keystore.p12'; File = Join-Path $tempDirectory 'city-service.cer' },
    @{ Alias = 'route-service'; Store = Join-Path $WildFlyHome 'standalone\configuration\route-service.p12'; File = Join-Path $tempDirectory 'route-service.cer' }
)

foreach ($certificate in $certificates) {
    & $keytool -exportcert -rfc -alias $certificate.Alias -keystore $certificate.Store -storepass $StorePassword -file $certificate.File
    if ($LASTEXITCODE -ne 0) { throw "Could not export $($certificate.Alias) certificate." }
    & certutil.exe -user -addstore Root $certificate.File | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "Could not trust $($certificate.Alias) certificate for the current Windows user." }
}

Write-Host 'The local HTTPS certificates are trusted for the current Windows user.'
