package ru.itmo.soa.city.repository;

import ru.itmo.soa.city.model.City;
import ru.itmo.soa.city.query.CityQuery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CityRepository {
    City create(City city);
    City update(City city);
    Optional<City> findById(int id);
    List<City> find(CityQuery query);
    List<City> findAll();
    boolean deleteById(int id);
    int deleteByMetersAboveSeaLevel(double value);
    boolean deleteOneByEstablishmentDate(LocalDateTime value);
}
