package ru.itmo.soa.city.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.hibernate.exception.ConstraintViolationException;
import ru.itmo.soa.city.exception.ApplicationException;
import ru.itmo.soa.city.exception.ConflictException;
import ru.itmo.soa.city.exception.RepositoryException;
import ru.itmo.soa.city.model.City;
import ru.itmo.soa.city.config.JpaEntityManagerProvider;
import ru.itmo.soa.city.query.CityQuery;
import ru.itmo.soa.city.query.SortCriterion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

@ApplicationScoped
public class JpaCityRepository implements CityRepository {
    private final JpaEntityManagerProvider entityManagerProvider;

    @Inject
    public JpaCityRepository(JpaEntityManagerProvider entityManagerProvider) {
        this.entityManagerProvider = entityManagerProvider;
    }

    @Override
    public City create(City city) {
        return inTransaction(entityManager -> {
            entityManager.persist(city);
            return city;
        });
    }

    @Override
    public City update(City city) {
        return inTransaction(entityManager -> entityManager.merge(city));
    }

    @Override
    public Optional<City> findById(int id) {
        return read(entityManager -> Optional.ofNullable(entityManager.find(City.class, id)));
    }

    @Override
    public List<City> find(CityQuery query) {
        return read(entityManager -> {
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            CriteriaQuery<City> criteriaQuery = criteriaBuilder.createQuery(City.class);
            Root<City> city = criteriaQuery.from(City.class);

            List<Predicate> predicates = buildPredicates(criteriaBuilder, city, query);
            criteriaQuery.where(predicates.toArray(Predicate[]::new));
            criteriaQuery.orderBy(buildOrders(criteriaBuilder, city, query.sort()));

            TypedQuery<City> typedQuery = entityManager.createQuery(criteriaQuery);
            typedQuery.setFirstResult((query.page() - 1) * query.size());
            typedQuery.setMaxResults(query.size());
            return typedQuery.getResultList();
        });
    }

    @Override
    public List<City> findAll() {
        return read(entityManager -> entityManager.createQuery(
                        "select c from City c order by c.id", City.class)
                .getResultList());
    }

    @Override
    public boolean deleteById(int id) {
        return inTransaction(entityManager -> {
            City city = entityManager.find(City.class, id);
            if (city == null) {
                return false;
            }
            entityManager.remove(city);
            return true;
        });
    }

    @Override
    public int deleteByMetersAboveSeaLevel(double value) {
        return inTransaction(entityManager -> entityManager.createQuery(
                        "delete from City c where c.metersAboveSeaLevel = :value")
                .setParameter("value", value)
                .executeUpdate());
    }

    @Override
    public boolean deleteOneByEstablishmentDate(LocalDateTime value) {
        return inTransaction(entityManager -> {
            List<City> cities = entityManager.createQuery(
                            "select c from City c where c.establishmentDate = :value order by c.id",
                            City.class)
                    .setParameter("value", value)
                    .setMaxResults(1)
                    .getResultList();
            if (cities.isEmpty()) {
                return false;
            }
            entityManager.remove(cities.get(0));
            return true;
        });
    }

    private List<Predicate> buildPredicates(CriteriaBuilder builder, Root<City> city, CityQuery query) {
        List<Predicate> predicates = new ArrayList<>();
        addEqualPredicate(predicates, builder, city.get("id"), query.id());
        addNamePrefixPredicate(predicates, builder, city.get("name"), query.name());
        addEqualPredicate(predicates, builder, city.get("creationDate"), query.creationDate());
        addEqualPredicate(predicates, builder, city.get("area"), query.area());
        addEqualPredicate(predicates, builder, city.get("population"), query.population());
        addEqualPredicate(predicates, builder, city.get("metersAboveSeaLevel"), query.metersAboveSeaLevel());
        addEqualPredicate(predicates, builder, city.get("establishmentDate"), query.establishmentDate());
        addEqualPredicate(predicates, builder, city.get("capital"), query.capital());
        addEqualPredicate(predicates, builder, city.get("climate"), query.climate());
        addEqualPredicate(predicates, builder, city.get("coordinates").get("x"), query.coordinatesX());
        addEqualPredicate(predicates, builder, city.get("coordinates").get("y"), query.coordinatesY());
        addEqualPredicate(predicates, builder, city.get("governor").get("birthday"), query.governorBirthday());
        return predicates;
    }

    private void addEqualPredicate(List<Predicate> predicates, CriteriaBuilder builder,
                                   Expression<?> expression, Object value) {
        if (value != null) {
            predicates.add(builder.equal(expression, value));
        }
    }

    private void addNamePrefixPredicate(List<Predicate> predicates, CriteriaBuilder builder,
                                        Expression<?> expression, String value) {
        if (value == null) {
            return;
        }

        String escapedValue = value.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        predicates.add(builder.like(
                builder.lower(expression.as(String.class)),
                escapedValue + "%",
                '\\'));
    }

    private List<Order> buildOrders(CriteriaBuilder builder, Root<City> city,
                                    List<SortCriterion> sortCriteria) {
        List<Order> orders = new ArrayList<>();
        if (sortCriteria.isEmpty()) {
            orders.add(builder.asc(city.get("id")));
            return orders;
        }

        for (SortCriterion criterion : sortCriteria) {
            Path<?> path = pathFor(city, criterion.field());
            orders.add(criterion.descending() ? builder.desc(path) : builder.asc(path));
        }

        if (sortCriteria.stream().noneMatch(criterion -> criterion.field().equals("id"))) {
            orders.add(builder.asc(city.get("id")));
        }
        return orders;
    }

    private Path<?> pathFor(Root<City> city, String field) {
        return switch (field) {
            case "id", "name", "creationDate", "area", "population", "metersAboveSeaLevel",
                 "establishmentDate", "capital", "climate" -> city.get(field);
            case "coordinates.x" -> city.get("coordinates").get("x");
            case "coordinates.y" -> city.get("coordinates").get("y");
            case "governor.birthday" -> city.get("governor").get("birthday");
            default -> throw new IllegalArgumentException("Неизвестное поле сортировки: " + field);
        };
    }

    private <T> T read(Function<EntityManager, T> operation) {
        try (EntityManager entityManager = entityManagerProvider.createEntityManager()) {
            return operation.apply(entityManager);
        } catch (RuntimeException exception) {
            throw databaseError("Ошибка чтения коллекции городов", exception);
        }
    }

    private <T> T inTransaction(Function<EntityManager, T> operation) {
        try (EntityManager entityManager = entityManagerProvider.createEntityManager()) {
            entityManager.getTransaction().begin();
            try {
                T result = operation.apply(entityManager);
                entityManager.getTransaction().commit();
                return result;
            } catch (RuntimeException exception) {
                if (entityManager.getTransaction().isActive()) {
                    entityManager.getTransaction().rollback();
                }
                throw exception;
            }
        } catch (RuntimeException exception) {
            throw databaseError("Ошибка изменения коллекции городов", exception);
        }
    }

    private ApplicationException databaseError(String message, RuntimeException exception) {
        if (hasConstraintViolation(exception)) {
            return new ConflictException(
                    "Нарушено ограничение целостности данных", exception);
        }
        if (exception instanceof RepositoryException repositoryException) {
            return repositoryException;
        }
        return new RepositoryException(message, exception);
    }

    private boolean hasConstraintViolation(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof ConstraintViolationException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
