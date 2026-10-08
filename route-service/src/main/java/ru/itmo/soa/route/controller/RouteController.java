package ru.itmo.soa.route.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.soa.route.dto.RouteResponse;
import ru.itmo.soa.route.service.RouteService;

@RestController
@RequestMapping(path = "/calculate", produces = MediaType.APPLICATION_JSON_VALUE)
public class RouteController {
    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping("/to-largest")
    public ResponseEntity<RouteResponse> toLargest() {
        return routeService.calculateToLargest()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.<RouteResponse>noContent().build());
    }

    @GetMapping("/to-oldest")
    public ResponseEntity<RouteResponse> toOldest() {
        return routeService.calculateToOldest()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.<RouteResponse>noContent().build());
    }
}
