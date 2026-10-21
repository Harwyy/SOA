package ru.itmo.soa.city.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import ru.itmo.soa.city.model.City;

public final class CityRepository {
    private static final CityRepository INSTANCE = new CityRepository();

    private final AtomicInteger cityIdSequence = new AtomicInteger();
    private final ConcurrentHashMap<Integer, City> citiesById = new ConcurrentHashMap<>();

    private CityRepository() {
    }

    public static CityRepository getInstance() {
        return INSTANCE;
    }

    public City save(City city) {
        if (city.getId() == 0) {
            city.setId(cityIdSequence.incrementAndGet());
        }
        citiesById.put(city.getId(), city);
        return city;
    }

    public City findById(Integer cityId) {
        return citiesById.get(cityId);
    }

    public City deleteById(Integer cityId) {
        return citiesById.remove(cityId);
    }

    public List<City> findAll() {
        return new ArrayList<>(citiesById.values());
    }

    public List<City> deleteAllBy(Predicate<City> condition) {
        List<City> deletedCities = new ArrayList<>();
        citiesById.forEach((cityId, city) -> {
            if (condition.test(city) && citiesById.remove(cityId, city)) {
                deletedCities.add(city);
            }
        });
        return deletedCities;
    }
}
