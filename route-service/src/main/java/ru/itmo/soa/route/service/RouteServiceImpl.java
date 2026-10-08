package ru.itmo.soa.route.service;

import org.springframework.stereotype.Service;
import ru.itmo.soa.route.client.CityClient;
import ru.itmo.soa.route.dto.CityResponse;
import ru.itmo.soa.route.dto.CoordinatesDto;
import ru.itmo.soa.route.dto.RouteResponse;
import ru.itmo.soa.route.exception.CityServiceUnavailableException;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

@Service
public class RouteServiceImpl implements RouteService {
    private final CityClient cityClient;

    public RouteServiceImpl(CityClient cityClient) {
        this.cityClient = cityClient;
    }

    @Override
    public Optional<RouteResponse> calculateToLargest() {
        return Arrays.stream(cityClient.findAll())
                .peek(this::validateCity)
                .max(Comparator.comparing(CityResponse::getArea))
                .map(this::toResult);
    }

    @Override
    public Optional<RouteResponse> calculateToOldest() {
        return Arrays.stream(cityClient.findAll())
                .peek(this::validateCity)
                .filter(city -> city.getEstablishmentDate() != null)
                .min(Comparator.comparing(CityResponse::getEstablishmentDate))
                .map(this::toResult);
    }

    private RouteResponse toResult(CityResponse city) {
        CoordinatesDto coordinates = city.getCoordinates();
        double length = Math.hypot(coordinates.getX(), coordinates.getY());
        return new RouteResponse(length, city.getId());
    }

    private void validateCity(CityResponse city) {
        if (city == null
                || city.getId() <= 0
                || city.getArea() == null
                || !Float.isFinite(city.getArea())
                || city.getArea() <= 0
                || city.getCoordinates() == null
                || city.getCoordinates().getX() == null
                || city.getCoordinates().getY() == null
                || !Float.isFinite(city.getCoordinates().getY())) {
            throw new CityServiceUnavailableException(
                    new IllegalStateException("Первый сервис вернул некорректный город"));
        }
    }
}
