package ru.itmo.soa.route.service;

import ru.itmo.soa.route.dto.RouteResponse;

import java.util.Optional;

public interface RouteService {
    Optional<RouteResponse> calculateToLargest();
    Optional<RouteResponse> calculateToOldest();
}
