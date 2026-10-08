# city-service

Первый вызываемый сервис лабораторной работы. Реализован на JAX-RS и разворачивается
как WAR в Payara.

## Технологии

- Java 21;
- Jakarta REST/JAX-RS;
- JPA и Hibernate;
- H2 в файловом режиме;
- Flyway для миграций;
- Bean Validation;
- Lombok.

## Слои

```text
controller/       JAX-RS endpoints и CORS
controller/error/ преобразование исключений в JSON-ответы
service/          бизнес-операции
repository/       интерфейс и JPA-реализация доступа к БД
model/            JPA-сущности City, Coordinates, Human, Climate
dto/              JSON-модели запросов, ответов и параметров поиска
query/            разбор фильтров, дат, сортировки и пагинации
mapper/           преобразование DTO и сущностей
config/           конфигурация EntityManager и Payara
exception/        прикладные исключения
resources/db/     миграции Flyway
```

## API

Базовый URL: `http://localhost:8080/city-service`.

```text
GET    /cities
POST   /cities
GET    /cities/{id}
PUT    /cities/{id}
DELETE /cities/{id}
DELETE /cities/by-meters-above-sea-level?metersAboveSeaLevel=...
DELETE /cities/by-establishment-date?establishmentDate=YYYY-MM-DDTHH:mm:ss
GET    /cities/average-meters-above-sea-level
```

`name` в фильтре ищется с начала строки без учёта регистра. Остальные фильтры
сравниваются по соответствующему типу поля. Поддерживаются `sort`, `page` и `size`.

## Сборка и развёртывание

```powershell
Push-Location .\city-service
mvn clean package
Pop-Location

& "$env:PAYARA_HOME\bin\asadmin.bat" deploy `
  --upload=true `
  "$PWD\city-service\target\city-service.war"
```

Перед развёртыванием Payara должен быть запущен и настроен командами из
корневого `README.md`.

## Конфигурация

`src/main/resources/META-INF/microprofile-config.properties` задаёт путь H2
`${user.home}/city-service-db`, пользователя БД `sa`, режим Hibernate `validate`
и расположение миграций Flyway.
