package ru.itmo.soa.city.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import ru.itmo.soa.city.dto.AverageMetersAboveSeaLevelResponse;
import ru.itmo.soa.city.dto.CityRequest;
import ru.itmo.soa.city.dto.CityResponse;
import ru.itmo.soa.city.dto.CitySearchParams;
import ru.itmo.soa.city.mapper.CityMapper;
import ru.itmo.soa.city.model.City;
import ru.itmo.soa.city.query.CityQuery;
import ru.itmo.soa.city.query.CityQueryParser;
import ru.itmo.soa.city.service.CityService;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
@Path("/cities")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CityController {
    private final CityQueryParser queryParser = new CityQueryParser();

    @Inject
    private CityService cityService;

    @GET
    public Response list(@Valid @BeanParam CitySearchParams params) {
        CityQuery query = queryParser.parse(params);
        List<CityResponse> cities = cityService.find(query).stream()
                .map(CityMapper::toResponse)
                .toList();
        return cities.isEmpty() ? Response.noContent().build() : Response.ok(cities).build();
    }

    @POST
    public Response create(@NotNull @Valid CityRequest input, @Context UriInfo uriInfo) {
        City city = cityService.create(input);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(String.valueOf(city.getId())).build())
                .entity(CityMapper.toResponse(city))
                .build();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") String value) {
        return Response.ok(CityMapper.toResponse(
                cityService.getById(queryParser.parseRequiredId(value)))).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") String value,
                           @NotNull @Valid CityRequest input) {
        City city = cityService.update(queryParser.parseRequiredId(value), input);
        return Response.ok(CityMapper.toResponse(city)).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String value) {
        cityService.delete(queryParser.parseRequiredId(value));
        return Response.noContent().build();
    }

    @DELETE
    @Path("/by-meters-above-sea-level")
    public Response deleteByMeters(@QueryParam("metersAboveSeaLevel") String value) {
        cityService.deleteAllByMetersAboveSeaLevel(
                queryParser.parseRequiredDouble(value, "metersAboveSeaLevel"));
        return Response.noContent().build();
    }

    @DELETE
    @Path("/by-establishment-date")
    public Response deleteByEstablishmentDate(@QueryParam("establishmentDate") String value) {
        LocalDateTime date = queryParser.parseRequiredLocalDateTime(value, "establishmentDate");
        cityService.deleteOneByEstablishmentDate(date);
        return Response.noContent().build();
    }

    @GET
    @Path("/average-meters-above-sea-level")
    public Response averageMetersAboveSeaLevel() {
        return cityService.averageMetersAboveSeaLevel()
                .map(value -> Response.ok(new AverageMetersAboveSeaLevelResponse(value)).build())
                .orElseGet(() -> Response.noContent().build());
    }

}
