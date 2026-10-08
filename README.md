# City Services

Лабораторная работа по SOA: два независимых REST-сервиса и React-клиент.

## Состав проекта

```text
city-service/   JAX-RS + JPA/Hibernate + H2 + Flyway, Payara
route-service/  Spring MVC REST, WildFly, HTTP-клиент к city-service
client-app/     React + Vite, пользовательский интерфейс
scripts/        скрипты настройки портов серверов
openapi.yaml    общая спецификация API
```

Общего Java-модуля и корневого `pom.xml` нет: каждый сервер собирается и запускается отдельно.
Описание модулей находится в `city-service/README.md`, `route-service/README.md` и
`client-app/README.md`.

## Требования

- JDK 21;
- Maven 3.9+;
- Node.js 18+ и npm;
- Payara Community 7;
- WildFly 41.

В локальной конфигурации используется HTTP:

```powershell
$env:PAYARA_HOME = "C:\Tools\payara7"
$env:WILDFLY_HOME = "C:\Tools\wildfly-41"
```

Проверьте окружение:

```powershell
java -version
mvn -version
node --version
npm --version
```

## Чистый запуск с нуля

Все команды ниже выполняются из корня проекта.

### 1. Сборка и запуск city-service

```powershell
Push-Location .\city-service
mvn clean package
Pop-Location

& "$env:PAYARA_HOME\bin\asadmin.bat" start-domain domain1
.\scripts\configure-payara.ps1

& "$env:PAYARA_HOME\bin\asadmin.bat" deploy `
  --force=true `
  "$PWD\city-service\target\city-service.war"
```

Проверка:

```powershell
curl.exe -i http://localhost:8080/city-service/cities
```

Ожидаемый адрес: `http://localhost:8080/city-service`.

### 2. Сборка и запуск route-service

В отдельном окне запустите WildFly:

```powershell
& "$env:WILDFLY_HOME\bin\standalone.bat" `
  "-Djboss.socket.binding.port-offset=1"
```

При offset `1` HTTP-порт приложения — `8081`, а management-порт — `9991`.
После полного запуска WildFly из корня проекта выполните:

```powershell
& "$env:WILDFLY_HOME\bin\jboss-cli.bat" `
  --controller=127.0.0.1:9991 `
  --connect `
  --file="$PWD\scripts\configure-wildfly.cli"
```

Соберите и разверните второй сервис:

```powershell
Push-Location .\route-service
mvn clean package
Pop-Location

Copy-Item .\route-service\target\route-service.war `
  "$env:WILDFLY_HOME\standalone\deployments\route-service.war" -Force
```

Проверка:

```powershell
curl.exe -i http://localhost:8081/route/calculate/to-largest
curl.exe -i http://localhost:8081/route/calculate/to-oldest
```

`route-service` по умолчанию вызывает `http://127.0.0.1:8080/city-service`.
Другой адрес задаётся так:

```powershell
$env:CITY_SERVICE_BASE_URL = "http://127.0.0.1:8080/city-service"
```

### 3. Запуск React-клиента

```powershell
Push-Location .\client-app
npm install
Copy-Item .env.example .env -Force
npm run dev
Pop-Location
```

Откройте `http://localhost:5173`.

Production-сборка:

```powershell
Push-Location .\client-app
npm run build
Pop-Location
```

## Возможности

`city-service` поддерживает CRUD, фильтрацию, поиск названия с начала без учёта регистра,
сортировку по нескольким полям, пагинацию, специальные удаления и среднее значение высоты.

`route-service` рассчитывает маршруты до крупнейшего и самого старого города.

Полная спецификация API находится в `openapi.yaml`.

## Даты

В React-клиенте пользовательский формат всех дат:

```text
гггг-мм-дд чч:мм
```

Клиент преобразует его в формат, который ожидают REST-сервисы.

## Данные H2

По умолчанию база city-service хранится в `%USERPROFILE%\city-service-db`.
Схема создаётся и обновляется миграциями Flyway. Hibernate работает в режиме проверки схемы.

## Остановка

```powershell
& "$env:PAYARA_HOME\bin\asadmin.bat" stop-domain domain1
```

WildFly и Vite останавливаются сочетанием `Ctrl+C` в соответствующих окнах.

## Временные каталоги

`city-service/target`, `route-service/target`, `client-app/node_modules` и `client-app/dist`
создаются автоматически и не являются исходными файлами проекта. Они исключены через `.gitignore`.
