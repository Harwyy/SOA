# SOA — лабораторная работа 1

Проект состоит из трёх приложений:

| Приложение | Каталог | Назначение | Результат сборки |
| --- | --- | --- | --- |
| City service | `city-service` | JAX-RS API коллекции городов для Payara | `city-service/target/city-service-1.0.0.war` |
| Route service | `route-service` | Spring MVC REST API маршрутов для WildFly; вызывает City service | `route-service/target/route-service-1.0.0.war` |
| Client | `client-react` | React-интерфейс для обоих API | `client-react/dist/` |

Все Java-исходники компилируются в формате **Java 17**. Версия Java новее допустима для локальной сборки, однако результат останется совместимым с Java 17.

## Что хранить в Git

В репозиторий добавляются исходники, `openapi.yaml`, `pom.xml`, `package.json`, `package-lock.json` и папка `scripts`. Папки `target`, `node_modules` и `dist` не добавляются: они пересоздаются командами ниже и уже исключены в `.gitignore`.

Перед первым коммитом можно удалить ранее собранные файлы:

```powershell
Remove-Item -Recurse -Force .\city-service\target, .\route-service\target, .\client-react\node_modules, .\client-react\dist -ErrorAction SilentlyContinue
```

Если репозиторий ещё не создан, минимальная последовательность такая:

```powershell
git init
git add .
git status
git commit -m "SOA laboratory work 1"
```

## Предварительные требования

На новом компьютере установите:

- JDK 17 и задайте `JAVA_HOME`;
- Maven 3.9 или новее;
- Node.js 20 LTS и npm;
- для локального запуска серверов: Payara 6 в `C:\Tools\payara6` и WildFly 41.0.1 в `C:\Tools\wildfly-41.0.1`.

Проверьте инструменты из корня проекта:

```powershell
java -version
mvn -version
node --version
npm --version
```

В выводе `java -version` должна быть Java 17. Если установлено несколько JDK, до сборки укажите нужную:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

## Сборка Java-сервисов и кодогенерация

Из корня проекта выполните одну команду:

```powershell
mvn clean verify
```

Maven автоматически выполняет всё необходимое:

1. Берёт контракт из `openapi.yaml` в корне репозитория.
2. Генерирует JAX-RS-контракт City service в `city-service/target/generated-openapi`.
3. Генерирует Spring MVC-контракт Route service в `route-service/target/generated-openapi`.
4. Компилирует код с `--release 17`.
5. Собирает два WAR-архива.

Ничего из `target/generated-openapi` вручную создавать или коммитить не нужно. Путь к спецификации относительный, поэтому кодогенерация работает после клонирования репозитория на другом компьютере.

Для сборки по отдельности:

```powershell
mvn -pl city-service -am clean package
mvn -pl route-service -am clean package
```

После успешной сборки должны существовать файлы:

```text
city-service\target\city-service-1.0.0.war
route-service\target\route-service-1.0.0.war
```

## Сборка React-клиента

Откройте вторую консоль в каталоге клиента:

```powershell
cd client-react
# Если vite уже запущен, сначала остановите его в другой консоли через Ctrl+C.
npm ci
npm run build
```

Готовые статические файлы появятся в `client-react/dist`. Для разработки и проверки интерфейса используйте:

```powershell
npm run dev
```

Интерфейс откроется по адресу `http://localhost:5500`. Vite перенаправляет запросы к API на HTTPS-сервисы:

- `/api/city` → `https://localhost:8181/city-service`;
- `/api/route` → `https://localhost:8443/route-service`.

Поэтому перед проверкой клиента должны быть запущены и развёрнуты оба Java-сервиса.

## Локальный запуск Java-сервисов на Windows

Из корня проекта после `mvn clean verify`:

```powershell
# 1. Настройка HTTPS Payara и запуск City service
.\scripts\configure-payara.ps1

# 2. Настройка HTTPS WildFly, доверия к сертификату Payara и запуск Route service
.\scripts\configure-wildfly.ps1

# 3. Развёртывание собранных WAR
.\scripts\deploy-services.ps1
```

Сервисы доступны только по HTTPS:

- City service: `https://localhost:8181/city-service/cities`;
- Route service: `https://localhost:8443/route-service/route/calculate/to-largest`.

Сертификаты самоподписанные, поэтому браузер, Postman или другой HTTP-клиент должны доверять им. Для текущего Windows-набора серверов используйте `scripts/trust-local-certificates.ps1`.

## Короткая последовательность на новом компьютере

```powershell
git clone <URL-РЕПОЗИТОРИЯ>
cd ЛР

mvn clean verify

cd client-react
npm ci
npm run build
cd ..

.\scripts\configure-payara.ps1
.\scripts\configure-wildfly.ps1
.\scripts\deploy-services.ps1
```

Затем, в отдельной консоли:

```powershell
cd client-react
npm run dev
```

## Проверка после сборки

Проверить, что WAR собраны для Java 17, можно командой:

```powershell
javap -verbose -classpath city-service\target\classes ru.itmo.soa.city.CityApplication | Select-String 'major version'
```

Ожидаемый результат: `major version: 61`.
