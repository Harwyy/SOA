package ru.itmo.soa.city.controller;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.List;
import ru.itmo.soa.city.mapper.CityMapper;
import ru.itmo.soa.city.repository.CityRepository;
import ru.itmo.soa.city.service.CityService;
import ru.itmo.soa.generated.city.model.CityInput;
import ru.itmo.soa.generated.city.model.Climate;

@Path("/cities")
@Consumes("application/json")
@Produces("application/json")
public class CityController {
    private final CityService cityService = new CityService(CityRepository.getInstance());

    @OPTIONS
    @Path("{path:.*}")
    public Response options() {
        return Response.noContent().build();
    }

    @POST
    public Response createCity(CityInput cityInput) {
        return Response.status(Response.Status.CREATED)
                .entity(CityMapper.toResponse(cityService.createCity(cityInput)))
                .build();
    }

    @GET
    @Path("/{id}")
    public Response getCity(@PathParam("id") Integer cityId) {
        return Response.ok(CityMapper.toResponse(cityService.getCity(cityId))).build();
    }

    @PUT
    @Path("/{id}")
    public Response updateCity(@PathParam("id") Integer cityId, CityInput cityInput) {
        return Response.ok(CityMapper.toResponse(cityService.updateCity(cityId, cityInput))).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteCity(@PathParam("id") Integer cityId) {
        cityService.deleteCity(cityId);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/by-meters-above-sea-level")
    public Response deleteCitiesByMetersAboveSeaLevel(
            @QueryParam("metersAboveSeaLevel") Float metersAboveSeaLevel) {
        cityService.deleteCitiesByMetersAboveSeaLevel(metersAboveSeaLevel);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/by-establishment-date")
    public Response deleteOneCityByEstablishmentDate(@QueryParam("establishmentDate") String establishmentDate) {
        cityService.deleteOneCityByEstablishmentDate(establishmentDate);
        return Response.noContent().build();
    }

    @GET
    @Path("/average-meters-above-sea-level")
    public Response calculateAverageMetersAboveSeaLevel() {
        return Response.ok(cityService.calculateAverageMetersAboveSeaLevel()).build();
    }

    @GET
    public Response getAllCities(
            @QueryParam("id") Integer id,
            @QueryParam("name") String name,
            @QueryParam("creationDate") String creationDate,
            @QueryParam("area") Long area,
            @QueryParam("population") Integer population,
            @QueryParam("metersAboveSeaLevel") Float metersAboveSeaLevel,
            @QueryParam("establishmentDate") String establishmentDate,
            @QueryParam("capital") Boolean capital,
            @QueryParam("climate") Climate climate,
            @QueryParam("coordinates.x") Double coordinatesX,
            @QueryParam("coordinates.y") Integer coordinatesY,
            @QueryParam("governor.birthday") java.util.Date governorBirthday,
            @QueryParam("sort") List<String> sort,
            @QueryParam("page") @DefaultValue("1") Integer page,
            @QueryParam("size") @DefaultValue("10") Integer size) {
        validatePagination(page, size);
        return Response.ok(cityService
                        .getAllCities(
                                id,
                                name,
                                creationDate,
                                area,
                                population,
                                metersAboveSeaLevel,
                                establishmentDate,
                                capital,
                                climate,
                                coordinatesX,
                                coordinatesY,
                                governorBirthday,
                                sort,
                                page,
                                size)
                        .stream()
                        .map(CityMapper::toResponse)
                        .toList())
                .build();
    }

    private void validatePagination(Integer page, Integer size) {
        if (page == null || page < 1) {
            throw new ru.itmo.soa.city.exception.CityException(
                    "page должен быть не меньше 1", Response.Status.BAD_REQUEST);
        }
        if (size == null || size < 1 || size > 100) {
            throw new ru.itmo.soa.city.exception.CityException(
                    "size должен быть от 1 до 100", Response.Status.BAD_REQUEST);
        }
    }
}
