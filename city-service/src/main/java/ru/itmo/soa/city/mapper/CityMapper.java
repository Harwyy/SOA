package ru.itmo.soa.city.mapper;

import ru.itmo.soa.city.dto.CityRequest;
import ru.itmo.soa.city.dto.CityResponse;
import ru.itmo.soa.city.dto.CoordinatesDto;
import ru.itmo.soa.city.dto.HumanDto;
import ru.itmo.soa.city.model.City;
import ru.itmo.soa.city.model.Coordinates;
import ru.itmo.soa.city.model.Human;

public final class CityMapper {
    private CityMapper() {
    }

    public static City toModel(CityRequest request) {
        City city = new City();
        city.setName(request.getName());
        city.setCoordinates(toModel(request.getCoordinates()));
        city.setArea(request.getArea());
        city.setPopulation(request.getPopulation());
        city.setMetersAboveSeaLevel(request.getMetersAboveSeaLevel());
        city.setEstablishmentDate(request.getEstablishmentDate());
        city.setCapital(request.isCapital());
        city.setClimate(request.getClimate());
        city.setGovernor(toModel(request.getGovernor()));
        return city;
    }

    public static CityResponse toResponse(City city) {
        return CityResponse.builder()
                .id(city.getId())
                .name(city.getName())
                .coordinates(toDto(city.getCoordinates()))
                .creationDate(city.getCreationDate())
                .area(city.getArea())
                .population(city.getPopulation())
                .metersAboveSeaLevel(city.getMetersAboveSeaLevel())
                .establishmentDate(city.getEstablishmentDate())
                .capital(city.isCapital())
                .climate(city.getClimate())
                .governor(toDto(city.getGovernor()))
                .build();
    }

    private static Coordinates toModel(CoordinatesDto coordinates) {
        if (coordinates == null) {
            return null;
        }
        return new Coordinates(coordinates.getX(), coordinates.getY());
    }

    private static Human toModel(HumanDto governor) {
        if (governor == null) {
            return null;
        }
        return new Human(governor.getBirthday());
    }

    private static CoordinatesDto toDto(Coordinates coordinates) {
        if (coordinates == null) {
            return null;
        }
        return new CoordinatesDto(coordinates.getX(), coordinates.getY());
    }

    private static HumanDto toDto(Human governor) {
        if (governor == null) {
            return null;
        }
        return new HumanDto(governor.getBirthday());
    }
}
