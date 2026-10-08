# route-service

Второй сервис лабораторной работы. Реализован на Spring MVC REST, разворачивается
как WAR в WildFly и получает города через REST API `city-service`.

## Технологии и слои

```text
controller/       REST endpoints маршрутов
controller/error/ обработка ошибок и ответ 502
service/          расчёт расстояния и выбор города
client/           HTTP-клиент первого сервиса
dto/              DTO городов и результата маршрута
config/           Spring MVC и загрузка application.properties
exception/        прикладные ошибки
```

## API

Базовый URL: `http://localhost:8081/route`.

```text
GET /calculate/to-largest
GET /calculate/to-oldest
```

При недоступности `city-service` сервис возвращает `502 Bad Gateway`.

## Сборка и развёртывание

```powershell
Push-Location .\route-service
mvn clean package
Pop-Location

Copy-Item .\route-service\target\route-service.war `
  "$env:WILDFLY_HOME\standalone\deployments\route-service.war" -Force
```

WildFly должен быть запущен с port offset `1`, чтобы не конфликтовать с Payara:

```powershell
& "$env:WILDFLY_HOME\bin\standalone.bat" `
  "-Djboss.socket.binding.port-offset=1"
```

В этом режиме HTTP-порт приложения — `8081`, management-порт — `9991`.
Полные команды настройки и развёртывания находятся в корневом `README.md`.

## Конфигурация

`src/main/resources/application.properties` содержит:

```properties
city.service.base-url=${CITY_SERVICE_BASE_URL:http://127.0.0.1:8080/city-service}
```

Для другого адреса первого сервиса задайте переменную `CITY_SERVICE_BASE_URL`.
