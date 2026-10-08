package ru.itmo.soa.city.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import ru.itmo.soa.city.dto.CityRequest;
import ru.itmo.soa.city.exception.CityNotFoundException;
import ru.itmo.soa.city.mapper.CityMapper;
import ru.itmo.soa.city.model.City;
import ru.itmo.soa.city.query.CityQuery;
import ru.itmo.soa.city.repository.CityRepository;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

@ApplicationScoped
public class CityServiceImpl implements CityService {
    private final CityRepository repository;

    @Inject
    public CityServiceImpl(CityRepository repository) {
        this.repository = repository;
    }

    @Override
    public City create(CityRequest input) {
        City city = CityMapper.toModel(input);
        city.setCreationDate(ZonedDateTime.now());
        return repository.create(city);
    }

    @Override
    public City getById(int id) {
        return repository.findById(id)
                .orElseThrow(() -> notFound(id));
    }

    @Override
    public City update(int id, CityRequest input) {
        City current = getById(id);
        City updated = CityMapper.toModel(input);
        updated.setId(current.getId());
        updated.setCreationDate(current.getCreationDate());
        return repository.update(updated);
    }

    @Override
    public void delete(int id) {
        if (!repository.deleteById(id)) {
            throw notFound(id);
        }
    }

    @Override
    public List<City> find(CityQuery query) {
        return repository.find(query);
    }

    @Override
    public void deleteAllByMetersAboveSeaLevel(double value) {
        if (repository.deleteByMetersAboveSeaLevel(value) == 0) {
            throw new CityNotFoundException("Города с заданной высотой нет");
        }
    }

    @Override
    public void deleteOneByEstablishmentDate(LocalDateTime value) {
        if (!repository.deleteOneByEstablishmentDate(value)) {
            throw new CityNotFoundException("Города с заданной датой основания нет");
        }
    }

    @Override
    public Optional<Double> averageMetersAboveSeaLevel() {
        OptionalDouble average = repository.findAll().stream()
                .map(City::getMetersAboveSeaLevel)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average();
        return average.isPresent() ? Optional.of(average.getAsDouble()) : Optional.empty();
    }

    private CityNotFoundException notFound(int id) {
        return new CityNotFoundException("Город с ID " + id + " не найден");
    }
}
