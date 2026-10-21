package ru.itmo.soa.city.mapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import ru.itmo.soa.city.model.City;
import ru.itmo.soa.city.model.Climate;
import ru.itmo.soa.city.model.Coordinates;
import ru.itmo.soa.city.model.Human;
import ru.itmo.soa.generated.city.model.CityInput;

public final class CityMapper {
    private CityMapper() {
    }

    public static City toEntity(CityInput cityInput, City previousCity) {
        City city = new City();
        city.setId(previousCity == null ? 0 : previousCity.getId());
        city.setCreationDate(previousCity == null ? LocalDateTime.now() : previousCity.getCreationDate());
        city.setName(cityInput.getName().trim());
        city.setCoordinates(toCoordinates(cityInput.getCoordinates()));
        city.setArea(cityInput.getArea());
        city.setPopulation(cityInput.getPopulation());
        city.setMetersAboveSeaLevel(cityInput.getMetersAboveSeaLevel());
        city.setEstablishmentDate(toZonedDateTime(cityInput.getEstablishmentDate()));
        city.setCapital(Boolean.TRUE.equals(cityInput.getCapital()));
        city.setClimate(cityInput.getClimate() == null ? null : Climate.valueOf(cityInput.getClimate().name()));
        city.setGovernor(toHuman(cityInput.getGovernor()));
        return city;
    }

    public static Map<String, Object> toResponse(City city) {
        Map<String, Object> cityResponse = new LinkedHashMap<>();
        cityResponse.put("id", city.getId());
        cityResponse.put("name", city.getName());

        Map<String, Object> coordinatesResponse = new LinkedHashMap<>();
        coordinatesResponse.put("x", city.getCoordinates().getX());
        coordinatesResponse.put("y", city.getCoordinates().getY());
        cityResponse.put("coordinates", coordinatesResponse);

        cityResponse.put("creationDate", city.getCreationDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        cityResponse.put("area", city.getArea());
        cityResponse.put("population", city.getPopulation());
        cityResponse.put("metersAboveSeaLevel", city.getMetersAboveSeaLevel());
        cityResponse.put("establishmentDate", city.getEstablishmentDate() == null
                ? null
                : city.getEstablishmentDate().format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        cityResponse.put("capital", city.isCapital());
        cityResponse.put("climate", city.getClimate());

        if (city.getGovernor() == null) {
            cityResponse.put("governor", null);
        } else {
            Map<String, Object> governorResponse = new LinkedHashMap<>();
            governorResponse.put("birthday", formatDate(city.getGovernor().getBirthday()));
            cityResponse.put("governor", governorResponse);
        }
        return cityResponse;
    }

    private static String formatDate(Date date) {
        return date == null ? null : DateTimeFormatter.ISO_INSTANT.format(date.toInstant());
    }

    private static Coordinates toCoordinates(ru.itmo.soa.generated.city.model.Coordinates cityCoordinates) {
        return new Coordinates(cityCoordinates.getX(), cityCoordinates.getY());
    }

    private static Human toHuman(ru.itmo.soa.generated.city.model.Human cityGovernor) {
        return cityGovernor == null ? null : new Human(cityGovernor.getBirthday());
    }

    private static ZonedDateTime toZonedDateTime(Date value) {
        return value == null ? null : value.toInstant().atZone(ZoneOffset.UTC);
    }
}
