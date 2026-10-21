param(
    [string]$PayaraHome = 'C:\Tools\payara6',
    [string]$WildFlyHome = 'C:\Tools\wildfly-41.0.1'
)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent

$asadmin = Join-Path $PayaraHome 'bin\asadmin.bat'
$cityWar = Join-Path $root 'city-service\target\city-service-1.0.0.war'
& $asadmin undeploy city-service 2>$null
& $asadmin deploy --name city-service --contextroot city-service $cityWar
if ($LASTEXITCODE -ne 0) { throw 'Could not deploy city-service to Payara.' }

$cli = Join-Path $WildFlyHome 'bin\jboss-cli.bat'
$routeWar = Join-Path $root 'route-service\target\route-service-1.0.0.war'
& $cli --connect --command='undeploy route-service.war' 2>$null
& $cli --connect --command="deploy `"$routeWar`" --name=route-service.war"
if ($LASTEXITCODE -ne 0) { throw 'Could not deploy route-service to WildFly.' }
Write-Host 'Both services are deployed.'
