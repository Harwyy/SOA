package ru.itmo.soa.city.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.flywaydb.core.Flyway;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import ru.itmo.soa.city.exception.RepositoryException;
import ru.itmo.soa.city.model.City;
import ru.itmo.soa.city.model.Coordinates;
import ru.itmo.soa.city.model.Human;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

@ApplicationScoped
public class JpaEntityManagerProvider {
    private SessionFactory sessionFactory;

    @Inject
    @ConfigProperty(name = "database.path")
    private String databasePath;

    @Inject
    @ConfigProperty(name = "database.user")
    private String databaseUser;

    @Inject
    @ConfigProperty(name = "database.password")
    private Optional<String> databasePassword;

    @Inject
    @ConfigProperty(name = "jpa.dialect")
    private String jpaDialect;

    @Inject
    @ConfigProperty(name = "jpa.ddl-auto")
    private String jpaDdlAuto;

    @Inject
    @ConfigProperty(name = "jpa.show-sql")
    private boolean jpaShowSql;

    @Inject
    @ConfigProperty(name = "jpa.format-sql")
    private boolean jpaFormatSql;

    @Inject
    @ConfigProperty(name = "database.migration.locations")
    private String migrationLocations;

    @Inject
    @ConfigProperty(name = "database.migration.baseline-on-migrate")
    private boolean migrationBaselineOnMigrate;

    @PostConstruct
    void initialize() {
        String resolvedDatabasePath = Path.of(databasePath)
                .toString()
                .replace('\\', '/');
        String jdbcUrl = "jdbc:h2:file:" + resolvedDatabasePath + ";DB_CLOSE_ON_EXIT=FALSE";

        Properties properties = new Properties();
        properties.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");
        properties.put("jakarta.persistence.jdbc.url", jdbcUrl);
        properties.put("jakarta.persistence.jdbc.user", databaseUser);
        properties.put("jakarta.persistence.jdbc.password", databasePassword.orElse(""));
        properties.put("hibernate.dialect", jpaDialect);
        properties.put("hibernate.hbm2ddl.auto", jpaDdlAuto);
        properties.put("hibernate.show_sql", jpaShowSql);
        properties.put("hibernate.format_sql", jpaFormatSql);
        properties.put("hibernate.type.json_format_mapper", DisabledFormatMapper.class.getName());
        properties.put("hibernate.type.xml_format_mapper", DisabledFormatMapper.class.getName());

        try {
            Flyway.configure()
                    .dataSource(jdbcUrl, databaseUser, databasePassword.orElse(""))
                    .locations(migrationLocations)
                    .baselineOnMigrate(migrationBaselineOnMigrate)
                    .load()
                    .migrate();

            sessionFactory = new Configuration()
                    .addAnnotatedClass(City.class)
                    .addAnnotatedClass(Coordinates.class)
                    .addAnnotatedClass(Human.class)
                    .addProperties(properties)
                    .buildSessionFactory();
        } catch (RuntimeException exception) {
            throw new RepositoryException("Не удалось инициализировать JPA/Hibernate", exception);
        }
    }

    @PreDestroy
    void close() {
        if (sessionFactory != null && sessionFactory.isOpen()) {
            sessionFactory.close();
        }
    }

    public EntityManager createEntityManager() {
        if (sessionFactory == null || !sessionFactory.isOpen()) {
            throw new RepositoryException("Фабрика EntityManager недоступна", null);
        }
        return sessionFactory.createEntityManager();
    }
}
