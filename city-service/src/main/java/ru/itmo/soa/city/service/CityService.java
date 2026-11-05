package ru.itmo.soa.city.service;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;
import ru.itmo.soa.city.exception.CityException;
import ru.itmo.soa.city.mapper.CityMapper;
import ru.itmo.soa.city.model.City;
import ru.itmo.soa.city.repository.CityRepository;
import ru.itmo.soa.city.time.CityDateTime;
import ru.itmo.soa.generated.city.model.AverageMetersAboveSeaLevel;
import ru.itmo.soa.generated.city.model.CityInput;
import ru.itmo.soa.generated.city.model.Coordinates;

/** Business rules for City entities stored by the Hibernate repository. */
public class CityService {
    private final CityRepository cityRepository;

    public CityService(CityRepository cityRepository) {
        this.cityRepository = cityRepository;
    }

    public City createCity(CityInput cityInput) {
        validateCityInput(cityInput);
        return cityRepository.save(CityMapper.toEntity(cityInput, null));
    }

    public City getCity(Integer cityId) {
        return findCityById(cityId);
    }

    public City updateCity(Integer cityId, CityInput cityInput) {
        validateCityInput(cityInput);
        return cityRepository.save(CityMapper.toEntity(cityInput, findCityById(cityId)));
    }

    public void deleteCity(Integer cityId) {
        validateCityId(cityId);
        if (cityRepository.deleteById(cityId) == null) {
            throw cityNotFound(cityId);
        }
    }

    public void deleteCitiesByMetersAboveSeaLevel(Float metersAboveSeaLevel) {
        if (metersAboveSeaLevel == null || !Float.isFinite(metersAboveSeaLevel)) {
            throw badRequest("metersAboveSeaLevel должен быть конечным числом");
        }
        List<City> deletedCities = cityRepository.deleteAllBy(
                city -> Float.compare(city.getMetersAboveSeaLevel(), metersAboveSeaLevel) == 0);
        if (deletedCities.isEmpty()) {
            throw new CityException("Городов с указанной высотой не найдено", Response.Status.NOT_FOUND);
        }
    }

    public void deleteOneCityByEstablishmentDate(String establishmentDate) {
        LocalDateTime requestedDate = CityDateTime.parse(establishmentDate, "establishmentDate");
        City cityToDelete = cityRepository.findAll().stream()
                .filter(city -> requestedDate.equals(city.getEstablishmentDate()))
                .findFirst()
                .orElseThrow(() -> new CityException(
                        "Городов с указанной датой основания не найдено", Response.Status.NOT_FOUND));
        cityRepository.deleteById(cityToDelete.getId());
    }

    public AverageMetersAboveSeaLevel calculateAverageMetersAboveSeaLevel() {
        var heightStatistics = cityRepository.findAll().stream()
                .mapToDouble(City::getMetersAboveSeaLevel)
                .summaryStatistics();
        if (heightStatistics.getCount() == 0) {
            throw new WebApplicationException(Response.noContent().build());
        }
        return new AverageMetersAboveSeaLevel().averageMetersAboveSeaLevel(heightStatistics.getAverage());
    }

    public List<City> getAllCities(
            Integer id, String name, String creationDate, Long area, Integer population,
            Float metersAboveSeaLevel, String establishmentDate, Boolean capital,
            ru.itmo.soa.generated.city.model.Climate climate, Double coordinatesX, Integer coordinatesY,
            String governorBirthday, List<String> sort, Integer page, Integer size) {
        validateFilters(area, population, metersAboveSeaLevel, coordinatesY);
        LocalDateTime requestedCreationDate = parseOptionalDate(creationDate, "creationDate");
        LocalDateTime requestedEstablishmentDate = parseOptionalDate(establishmentDate, "establishmentDate");
        LocalDateTime requestedGovernorBirthday = parseOptionalDate(governorBirthday, "governor.birthday");

        Stream<City> cities = cityRepository.findAll().stream()
                .filter(city -> matchesFilters(city, id, name, requestedCreationDate, area, population,
                        metersAboveSeaLevel, requestedEstablishmentDate, capital, climate, coordinatesX,
                        coordinatesY, requestedGovernorBirthday));
        Comparator<City> cityComparator = createComparator(sort);
        if (cityComparator != null) {
            cities = cities.sorted(cityComparator);
        }
        List<City> cityPage = cities.skip((long) (page - 1) * size).limit(size).toList();
        if (cityPage.isEmpty()) {
            throw new WebApplicationException(Response.noContent().build());
        }
        return cityPage;
    }

    private City findCityById(Integer cityId) {
        validateCityId(cityId);
        City city = cityRepository.findById(cityId);
        if (city == null) {
            throw cityNotFound(cityId);
        }
        return city;
    }

    private boolean matchesFilters(
            City city, Integer id, String name, LocalDateTime creationDate, Long area, Integer population,
            Float metersAboveSeaLevel, LocalDateTime establishmentDate, Boolean capital,
            ru.itmo.soa.generated.city.model.Climate climate, Double coordinatesX, Integer coordinatesY,
            LocalDateTime governorBirthday) {
        return (id == null || city.getId().equals(id))
                && (name == null || city.getName().toLowerCase(Locale.ROOT).startsWith(name.toLowerCase(Locale.ROOT)))
                && (creationDate == null || city.getCreationDate().equals(creationDate))
                && (area == null || city.getArea() == area)
                && (population == null || city.getPopulation() == population)
                && (metersAboveSeaLevel == null
                        || Float.compare(city.getMetersAboveSeaLevel(), metersAboveSeaLevel) == 0)
                && (establishmentDate == null || establishmentDate.equals(city.getEstablishmentDate()))
                && (capital == null || city.isCapital() == capital)
                && (climate == null || climate.name().equals(String.valueOf(city.getClimate())))
                && (coordinatesX == null || city.getCoordinates().getX().equals(coordinatesX))
                && (coordinatesY == null || city.getCoordinates().getY().equals(coordinatesY))
                && (governorBirthday == null
                        || (city.getGovernor() != null
                                && Objects.equals(city.getGovernor().getBirthday(), governorBirthday)));
    }

    private void validateCityInput(CityInput cityInput) {
        if (cityInput == null) {
            throw badRequest("Тело запроса обязательно");
        }
        if (cityInput.getName() == null || cityInput.getName().trim().isEmpty()) {
            throw badRequest("name не может быть пустым");
        }
        validateCoordinates(cityInput.getCoordinates());
        if (cityInput.getArea() == null || cityInput.getArea() <= 0) {
            throw badRequest("area должна быть больше 0");
        }
        if (cityInput.getPopulation() == null || cityInput.getPopulation() <= 0) {
            throw badRequest("population должно быть больше 0");
        }
        if (cityInput.getMetersAboveSeaLevel() == null || !Float.isFinite(cityInput.getMetersAboveSeaLevel())) {
            throw badRequest("metersAboveSeaLevel должен быть конечным числом");
        }
        parseOptionalDate(cityInput.getEstablishmentDate(), "establishmentDate");
        if (cityInput.getGovernor() != null) {
            parseOptionalDate(cityInput.getGovernor().getBirthday(), "governor.birthday");
        }
    }

    private void validateCoordinates(Coordinates coordinates) {
        if (coordinates == null || coordinates.getX() == null || coordinates.getY() == null) {
            throw badRequest("coordinates.x и coordinates.y обязательны");
        }
        if (!Double.isFinite(coordinates.getX())) {
            throw badRequest("coordinates.x должен быть конечным числом");
        }
        if (coordinates.getY() > 952) {
            throw badRequest("coordinates.y не может быть больше 952");
        }
    }

    private void validateFilters(Long area, Integer population, Float metersAboveSeaLevel, Integer coordinatesY) {
        if (area != null && area <= 0) {
            throw badRequest("area должна быть больше 0");
        }
        if (population != null && population <= 0) {
            throw badRequest("population должно быть больше 0");
        }
        if (metersAboveSeaLevel != null && !Float.isFinite(metersAboveSeaLevel)) {
            throw badRequest("metersAboveSeaLevel должен быть конечным числом");
        }
        if (coordinatesY != null && coordinatesY > 952) {
            throw badRequest("coordinates.y не может быть больше 952");
        }
    }

    private void validateCityId(Integer cityId) {
        if (cityId == null || cityId < 1) {
            throw badRequest("id должен быть положительным числом");
        }
    }

    private LocalDateTime parseOptionalDate(String value, String fieldName) {
        return value == null || value.isBlank() ? null : CityDateTime.parse(value, fieldName);
    }

    private Comparator<City> createComparator(List<String> sortValues) {
        if (sortValues == null || sortValues.isEmpty()) {
            return null;
        }
        Comparator<City> resultComparator = null;
        for (String sortValue : sortValues) {
            String[] sortParts = sortValue.split(",", -1);
            if (sortParts.length != 2 || !(sortParts[1].equals("asc") || sortParts[1].equals("desc"))) {
                throw badRequest("Некорректный sort");
            }
            Comparator<City> fieldComparator = createFieldComparator(sortParts[0]);
            if (fieldComparator == null) {
                throw badRequest("Неизвестное поле сортировки: " + sortParts[0]);
            }
            if (sortParts[1].equals("desc")) {
                fieldComparator = fieldComparator.reversed();
            }
            resultComparator = resultComparator == null
                    ? fieldComparator
                    : resultComparator.thenComparing(fieldComparator);
        }
        return resultComparator;
    }

    private Comparator<City> createFieldComparator(String fieldName) {
        return switch (fieldName) {
            case "id" -> Comparator.comparingInt(City::getId);
            case "name" -> Comparator.comparing(City::getName, String.CASE_INSENSITIVE_ORDER);
            case "creationDate" -> Comparator.comparing(City::getCreationDate);
            case "area" -> Comparator.comparingLong(City::getArea);
            case "population" -> Comparator.comparingInt(City::getPopulation);
            case "metersAboveSeaLevel" -> Comparator.comparingDouble(City::getMetersAboveSeaLevel);
            case "establishmentDate" -> Comparator.comparing(
                    City::getEstablishmentDate, Comparator.nullsLast(Comparator.naturalOrder()));
            case "capital" -> Comparator.comparing(City::isCapital);
            case "climate" -> Comparator.comparing(
                    City::getClimate, Comparator.nullsLast(Comparator.naturalOrder()));
            case "coordinates.x" -> Comparator.comparing(city -> city.getCoordinates().getX());
            case "coordinates.y" -> Comparator.comparing(city -> city.getCoordinates().getY());
            case "governor.birthday" -> Comparator.comparing(
                    city -> city.getGovernor() == null ? null : city.getGovernor().getBirthday(),
                    Comparator.nullsLast(Comparator.naturalOrder()));
            default -> null;
        };
    }

    private CityException cityNotFound(Integer cityId) {
        return new CityException("Город с ID " + cityId + " не найден", Response.Status.NOT_FOUND);
    }

    private CityException badRequest(String message) {
        return new CityException(message, Response.Status.BAD_REQUEST);
    }
}
