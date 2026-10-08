package ru.itmo.soa.route.client;

import ru.itmo.soa.route.dto.CityResponse;

public interface CityClient {
    CityResponse[] findAll();
}
