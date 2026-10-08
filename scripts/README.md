# scripts

Скрипты настройки локального запуска серверов.

## configure-payara.ps1

Настраивает Payara на HTTP-порт `8080`, отключает HTTPS-listener для локального
режима и перезапускает домен.

```powershell
.\scripts\configure-payara.ps1
```

Если `PAYARA_HOME` не задан, путь можно передать явно:

```powershell
.\scripts\configure-payara.ps1 -PayaraHome "C:\Tools\payara7"
```

## configure-wildfly.cli

Настраивает HTTP-listener WildFly на порт `8081` и выполняет reload.

```powershell
& "$env:WILDFLY_HOME\bin\jboss-cli.bat" `
  --controller=127.0.0.1:9991 `
  --connect `
  --file="$PWD\scripts\configure-wildfly.cli"
```

Перед этим WildFly запускается с параметром `-Djboss.socket.binding.port-offset=1`.
Скрипты не собирают и не разворачивают WAR-файлы. Порядок запуска описан в корневом README.
