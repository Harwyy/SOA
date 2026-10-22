# SOA — запуск на Helios

## Схема развёртывания

Сервисы запускаются на разных аккаунтах. Общий скрипт `start-services.sh` не используй: он рассчитан на Payara и WildFly в одном аккаунте.

| Сервис | Аккаунт | HTTPS-адрес |
| --- | --- | --- |
| City service (Payara) | `s373336` | `https://helios.se.ifmo.ru:18081/city-service` |
| Route service (WildFly) | `s368420` | `https://helios.se.ifmo.ru:20000/route-service` |
| UI | `s368420` | `https://se.ifmo.ru/~s368420/` |

Внешний доступ к портам сервисов может быть закрыт. Это нормально: UI обращается к API через `api.php` на том же домене. Проверяй API через SSH с аккаунта, где работает сервис.

## Файлы на сервере

WAR-архивы собираются на ноутбуке, не на Helios. Перед запуском должны существовать:

| Домашняя папка | Файлы |
| --- | --- |
| `/home/studs/s373336` | `city-service-1.0.0.war`, `scripts/linux/` |
| `/home/studs/s368420` | `route-service-1.0.0.war`, `city-service.pem`, `scripts/linux/` |

`city-service.pem` — сертификат City service, нужный WildFly для HTTPS-вызовов. Не удаляй его.

## 1. City service: Payara

```bash
ssh s373336@helios.se.ifmo.ru -p 2222
cd /home/studs/s373336/scripts/linux
```

В `config.local.sh` должны быть значения:

```bash
PAYARA_HOME="/home/studs/s373336/payara6"
PAYARA_DOMAIN="soa"
PAYARA_ADMIN_PORT="18048"
CITY_SERVICE_HOST="helios.se.ifmo.ru"
CITY_SERVICE_PORT="18081"
CITY_WAR_RELATIVE_PATH="city-service-1.0.0.war"
CERTIFICATE_DNS_NAME="helios.se.ifmo.ru"
CERTIFICATE_IP_ADDRESS="127.0.0.1"
JAVA_INITIAL_HEAP="32m"
JAVA_MAX_HEAP="192m"
JAVA_ACTIVE_PROCESSORS="1"
JAVA_COMPRESSED_CLASS_SPACE="64m"
JAVA_MAX_METASPACE="192m"
JAVA_THREAD_STACK="160k"
PAYARA_HTTP_MAX_THREADS="4"
PAYARA_ADMIN_MAX_THREADS="2"
PAYARA_ORB_MAX_THREADS="4"
PAYARA_THREAD_QUEUE_SIZE="32"
```

Запуск или повторное развёртывание:

```bash
bash start-city-service.sh
```

Проверка:

```bash
curl -k -i 'https://127.0.0.1:18081/city-service/cities?page=1&size=10'
```

Ожидается `HTTP/2 200` (есть города) или `HTTP/2 204` (коллекция пуста).

Логи при ошибке:

```bash
tail -n 100 /home/studs/s373336/payara6/glassfish/domains/soa/logs/soa-startup.log
tail -n 100 /home/studs/s373336/payara6/glassfish/domains/soa/logs/server.log
```

## 2. Route service: WildFly

Запускай после успешного запуска City service.

```bash
ssh s368420@se.ifmo.ru -p 2222
cd /home/studs/s368420/scripts/linux
```

В `config.local.sh` должны быть значения:

```bash
WILDFLY_HOME="/home/studs/s368420/wildfly-41.0.1"
WILDFLY_BIND_ADDRESS="0.0.0.0"
CITY_SERVICE_HOST="helios.se.ifmo.ru"
CITY_SERVICE_PORT="18081"
ROUTE_SERVICE_PORT="20000"
ROUTE_WAR_RELATIVE_PATH="route-service-1.0.0.war"
CITY_CERTIFICATE_FILE="/home/studs/s368420/city-service.pem"
CERTIFICATE_DNS_NAME="helios.se.ifmo.ru"
CERTIFICATE_IP_ADDRESS="127.0.0.1"
JAVA_INITIAL_HEAP="32m"
JAVA_MAX_HEAP="192m"
JAVA_ACTIVE_PROCESSORS="1"
JAVA_COMPRESSED_CLASS_SPACE="64m"
JAVA_MAX_METASPACE="192m"
JAVA_THREAD_STACK="160k"
WILDFLY_JCA_CORE_THREADS="1"
WILDFLY_JCA_MAX_THREADS="1"
WILDFLY_JCA_QUEUE_LENGTH="1"
```

Запуск или повторное развёртывание:

```bash
bash start-route-service.sh
```

Проверка:

```bash
curl -k -i 'https://127.0.0.1:20000/route-service/route/calculate/to-largest'
```

Ожидается `HTTP/2 200` с маршрутом или `HTTP/2 204`, если в City service нет подходящего города.

Логи при ошибке:

```bash
tail -n 100 /home/studs/s368420/wildfly-41.0.1/standalone/log/soa-startup.log
tail -n 100 /home/studs/s368420/wildfly-41.0.1/standalone/log/server.log
```

## 3. UI

Открой `https://se.ifmo.ru/~s368420/`. Если видна старая версия, нажми `Ctrl + F5`.

## Обновление WAR

1. На ноутбуке пересобери WAR-архивы.
2. Замени `city-service-1.0.0.war` в `/home/studs/s373336/` и выполни:

   ```bash
   bash /home/studs/s373336/scripts/linux/start-city-service.sh
   ```

3. Замени `route-service-1.0.0.war` в `/home/studs/s368420/` и выполни:

   ```bash
   bash /home/studs/s368420/scripts/linux/start-route-service.sh
   ```

4. Выполни проверки `curl -k` из разделов выше и открой UI.

## Остановка

```bash
# аккаунт s373336
/home/studs/s373336/payara6/bin/asadmin --port 18048 stop-domain soa

# аккаунт s368420
/home/studs/s368420/wildfly-41.0.1/bin/jboss-cli.sh --connect --command=':shutdown'
```
