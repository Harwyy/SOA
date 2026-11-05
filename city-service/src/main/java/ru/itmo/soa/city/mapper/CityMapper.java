package ru.itmo.soa.city.mapper;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import ru.itmo.soa.city.model.City;
import ru.itmo.soa.city.model.Climate;
import ru.itmo.soa.city.model.Coordinates;
import ru.itmo.soa.city.model.Human;
import ru.itmo.soa.city.time.CityDateTime;
import ru.itmo.soa.generated.city.model.CityInput;

/** Converts generated API DTOs to persistent entities and back. */
public final class CityMapper {
    private CityMapper() {
    }

    public static City toEntity(CityInput cityInput, City previousCity) {
        City city = new City();
        city.setId(previousCity == null ? null : previousCity.getId());
        city.setCreationDate(previousCity == null ? LocalDateTime.now() : previousCity.getCreationDate());
        city.setName(cityInput.getName().trim());
        city.setCoordinates(toCoordinates(cityInput.getCoordinates()));
        city.setArea(cityInput.getArea());
        city.setPopulation(cityInput.getPopulation());
        city.setMetersAboveSeaLevel(cityInput.getMetersAboveSeaLevel());
        city.setEstablishmentDate(parseOptional(cityInput.getEstablishmentDate(), "establishmentDate"));
        city.setCapital(Boolean.TRUE.equals(cityInput.getCapital()));
        city.setClimate(cityInput.getClimate() == null ? null : Climate.valueOf(cityInput.getClimate().name()));
        city.setGovernor(toHuman(cityInput.getGovernor()));
        return city;
    }

    public static Map<String, Object> toResponse(City city) {
        Map<String, Object> cityResponse = new LinkedHashMap<>();
        cityResponse.put("id", city.getId());
        cityResponse.put("name", city.getName());
        cityResponse.put("coordinates", Map.of(
                "x", city.getCoordinates().getX(),
                "y", city.getCoordinates().getY()));
        cityResponse.put("creationDate", CityDateTime.format(city.getCreationDate()));
        cityResponse.put("area", city.getArea());
        cityResponse.put("population", city.getPopulation());
        cityResponse.put("metersAboveSeaLevel", city.getMetersAboveSeaLevel());
        cityResponse.put("establishmentDate", CityDateTime.format(city.getEstablishmentDate()));
        cityResponse.put("capital", city.isCapital());
        cityResponse.put("climate", city.getClimate());
        if (city.getGovernor() == null) {
            cityResponse.put("governor", null);
        } else {
            Map<String, Object> governorResponse = new LinkedHashMap<>();
            governorResponse.put("birthday", CityDateTime.format(city.getGovernor().getBirthday()));
            cityResponse.put("governor", governorResponse);
        }
        return cityResponse;
    }

    private static Coordinates toCoordinates(ru.itmo.soa.generated.city.model.Coordinates coordinates) {
        return new Coordinates(coordinates.getX(), coordinates.getY());
    }

    private static Human toHuman(ru.itmo.soa.generated.city.model.Human governor) {
        return governor == null ? null : new Human(parseOptional(governor.getBirthday(), "governor.birthday"));
    }

    private static LocalDateTime parseOptional(String value, String fieldName) {
        return value == null ? null : CityDateTime.parse(value, fieldName);
    }
}
