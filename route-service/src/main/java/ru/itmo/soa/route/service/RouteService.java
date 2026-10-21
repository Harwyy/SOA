package ru.itmo.soa.route.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import ru.itmo.soa.generated.route.model.RouteResult;
import ru.itmo.soa.route.client.CityServiceClient;

@Service
public class RouteService {
    private final CityServiceClient cityServiceClient;

    public RouteService(CityServiceClient cityServiceClient) {
        this.cityServiceClient = cityServiceClient;
    }

    public RouteResult calculateRouteToLargestCity() {
        List<JsonNode> cities = cityServiceClient.getAllCities();
        return cities.stream()
                .filter(this::hasAreaAndCoordinates)
                .max(Comparator.comparingDouble(city -> city.path("area").asDouble()))
                .map(this::createRouteResult)
                .orElse(null);
    }

    public RouteResult calculateRouteToOldestCity() {
        List<JsonNode> cities = cityServiceClient.getAllCities();
        return cities.stream()
                .filter(this::hasEstablishmentDateAndCoordinates)
                .min(Comparator.comparing(city -> ZonedDateTime.parse(city.path("establishmentDate").asText())))
                .map(this::createRouteResult)
                .orElse(null);
    }

    private boolean hasAreaAndCoordinates(JsonNode city) {
        return city.hasNonNull("area") && city.hasNonNull("coordinates");
    }

    private boolean hasEstablishmentDateAndCoordinates(JsonNode city) {
        return city.hasNonNull("establishmentDate") && city.hasNonNull("coordinates");
    }

    private RouteResult createRouteResult(JsonNode city) {
        JsonNode coordinates = city.path("coordinates");
        double coordinateX = coordinates.path("x").asDouble();
        double coordinateY = coordinates.path("y").asDouble();
        double routeLength = Math.sqrt(coordinateX * coordinateX + coordinateY * coordinateY);
        return new RouteResult().cityId(city.path("id").asInt()).length(routeLength);
    }
}
