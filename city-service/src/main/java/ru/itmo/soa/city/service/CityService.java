package ru.itmo.soa.city.service;

import ru.itmo.soa.city.dto.CityRequest;
import ru.itmo.soa.city.model.City;
import ru.itmo.soa.city.query.CityQuery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CityService {
    City create(CityRequest input);
    City getById(int id);
    City update(int id, CityRequest input);
    void delete(int id);
    List<City> find(CityQuery query);
    void deleteAllByMetersAboveSeaLevel(double value);
    void deleteOneByEstablishmentDate(LocalDateTime value);
    Optional<Double> averageMetersAboveSeaLevel();
}
