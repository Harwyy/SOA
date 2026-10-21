package ru.itmo.soa.route.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.soa.generated.route.api.RouteApi;
import ru.itmo.soa.generated.route.model.RouteResult;
import ru.itmo.soa.route.service.RouteService;

@RestController
public class RouteController implements RouteApi {
    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @Override
    public ResponseEntity<RouteResult> calculateRouteToLargestCity() {
        RouteResult routeResult = routeService.calculateRouteToLargestCity();
        return routeResult == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(routeResult);
    }

    @Override
    public ResponseEntity<RouteResult> calculateRouteToOldestCity() {
        RouteResult routeResult = routeService.calculateRouteToOldestCity();
        return routeResult == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(routeResult);
    }
}
