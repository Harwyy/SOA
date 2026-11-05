package ru.itmo.soa.city.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceException;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import ru.itmo.soa.city.model.City;

/** Hibernate-backed repository for the City collection. */
public final class CityRepository {
    private static final CityRepository INSTANCE = new CityRepository();

    private volatile EntityManagerFactory entityManagerFactory;

    private CityRepository() {
    }

    public static CityRepository getInstance() {
        return INSTANCE;
    }

    public City save(City city) {
        return inTransaction(entityManager -> {
            if (city.getId() == null) {
                entityManager.persist(city);
                return city;
            }
            return entityManager.merge(city);
        });
    }

    public City findById(Integer cityId) {
        return read(entityManager -> entityManager.find(City.class, cityId));
    }

    public City deleteById(Integer cityId) {
        return inTransaction(entityManager -> {
            City city = entityManager.find(City.class, cityId);
            if (city != null) {
                entityManager.remove(city);
            }
            return city;
        });
    }

    public List<City> findAll() {
        return read(entityManager -> entityManager
                .createQuery("select city from City city", City.class)
                .getResultList());
    }

    public List<City> deleteAllBy(Predicate<City> condition) {
        return inTransaction(entityManager -> {
            List<City> cities = entityManager
                    .createQuery("select city from City city", City.class)
                    .getResultList();
            List<City> deleted = cities.stream().filter(condition).toList();
            deleted.forEach(entityManager::remove);
            return deleted;
        });
    }

    private <T> T read(Function<EntityManager, T> operation) {
        EntityManager entityManager = null;
        try {
            entityManager = entityManagerFactory().createEntityManager();
            return operation.apply(entityManager);
        } catch (PersistenceException exception) {
            throw new CityStorageException("Не удалось выполнить операцию с базой данных", exception);
        } finally {
            close(entityManager);
        }
    }

    private <T> T inTransaction(Function<EntityManager, T> operation) {
        EntityManager entityManager = null;
        try {
            entityManager = entityManagerFactory().createEntityManager();
            entityManager.getTransaction().begin();
            T result = operation.apply(entityManager);
            entityManager.getTransaction().commit();
            return result;
        } catch (PersistenceException exception) {
            rollback(entityManager);
            throw new CityStorageException("Не удалось сохранить изменения в базе данных", exception);
        } catch (RuntimeException exception) {
            rollback(entityManager);
            throw exception;
        } finally {
            close(entityManager);
        }
    }

    private void rollback(EntityManager entityManager) {
        if (entityManager != null && entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().rollback();
        }
    }

    private void close(EntityManager entityManager) {
        if (entityManager != null && entityManager.isOpen()) {
            entityManager.close();
        }
    }

    private EntityManagerFactory entityManagerFactory() {
        EntityManagerFactory current = entityManagerFactory;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (entityManagerFactory == null) {
                try {
                    entityManagerFactory = Persistence.createEntityManagerFactory(
                            "city-persistence-unit", DatabaseConfiguration.createProperties());
                } catch (PersistenceException exception) {
                    throw new CityStorageException("Не удалось подключиться к базе данных", exception);
                }
            }
            return entityManagerFactory;
        }
    }
}
