package ru.itmo.soa.city.repository;

import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.persistence.PersistenceException;

final class DatabaseConfiguration {
    private DatabaseConfiguration() {
    }

    static Map<String, Object> createProperties() {
        String url = value("city.db.url", "CITY_DB_URL");
        String user = value("city.db.user", "CITY_DB_USER");
        String password = value("city.db.password", "CITY_DB_PASSWORD");
        if (isBlank(url) || isBlank(user) || isBlank(password)) {
            throw new PersistenceException(
                    "База данных не настроена. Укажите city.db.url, city.db.user и city.db.password "
                            + "как JVM properties либо CITY_DB_URL, CITY_DB_USER и CITY_DB_PASSWORD в окружении.");
        }

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("jakarta.persistence.jdbc.url", url);
        properties.put("jakarta.persistence.jdbc.user", user);
        properties.put("jakarta.persistence.jdbc.password", password);
        properties.put("jakarta.persistence.jdbc.driver", "org.postgresql.Driver");
        properties.put("hibernate.dialect", valueOrDefault("city.db.dialect", "CITY_DB_DIALECT",
                "org.hibernate.dialect.PostgreSQLDialect"));
        properties.put("hibernate.hbm2ddl.auto", valueOrDefault("city.db.schema-mode", "CITY_DB_SCHEMA_MODE", "update"));
        properties.put("hibernate.show_sql", "false");
        properties.put("hibernate.format_sql", "false");
        return properties;
    }

    private static String value(String systemProperty, String environmentVariable) {
        String configured = System.getProperty(systemProperty);
        return isBlank(configured) ? System.getenv(environmentVariable) : configured;
    }

    private static String valueOrDefault(String systemProperty, String environmentVariable, String defaultValue) {
        String configured = value(systemProperty, environmentVariable);
        return isBlank(configured) ? defaultValue : configured;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
