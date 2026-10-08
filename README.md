# City Services

- `city-service` — REST API городов, Payara, порт `8080`;
- `route-service` — расчёт маршрутов, WildFly, порт `8081`;
- `client-app` — React-клиент, порт `5173`.

API: [документация](https://harwyy.github.io/SOA/) · [openapi.yaml](./openapi.yaml)

Для публикации выбрать в GitHub: `Settings → Pages → Deploy from a branch → main /docs`.

## Окружение

```powershell
$env:PROJECT_HOME = "C:\Users\Honor\Desktop\SOA"
$env:JAVA_HOME = "C:\Tools\jdk-21"
$env:PAYARA_HOME = "C:\Tools\payara7-server"
$env:WILDFLY_HOME = "C:\Tools\wildfly41"
$env:NODE_HOME = "C:\Tools\nodejs"
```

## Сборка

```powershell
cd $env:PROJECT_HOME

mvn -f .\city-service\pom.xml clean package
mvn -f .\route-service\pom.xml clean package

cd .\client-app
& "$env:NODE_HOME\npm.cmd" ci
```

## city-service

```powershell
& "$env:PAYARA_HOME\bin\asadmin.bat" start-domain domain1
& "$env:PAYARA_HOME\bin\asadmin.bat" deploy --upload=true `
    "$env:PROJECT_HOME\city-service\target\city-service.war"
```

API: http://127.0.0.1:8080/city-service

## route-service

```powershell
& "$env:WILDFLY_HOME\bin\standalone.bat" `
    "-Djboss.socket.binding.port-offset=1"
```

```powershell
& "$env:WILDFLY_HOME\bin\jboss-cli.bat" `
    --controller=127.0.0.1:9991 `
    --connect `
    --command="deploy $env:PROJECT_HOME\route-service\target\route-service.war --force"
```

API: http://127.0.0.1:8081/route

## client-app

```powershell
cd "$env:PROJECT_HOME\client-app"
& "$env:NODE_HOME\npm.cmd" run dev -- --host 127.0.0.1 --port 5173
```

Клиент: http://127.0.0.1:5173
