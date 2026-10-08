package ru.itmo.soa.city.dto;

import jakarta.validation.constraints.Size;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CitySearchParams {
    @QueryParam("id")
    private String id;

    @QueryParam("name")
    @Size(min = 1)
    private String name;

    @QueryParam("creationDate")
    private String creationDate;

    @QueryParam("area")
    private String area;

    @QueryParam("population")
    private String population;

    @QueryParam("metersAboveSeaLevel")
    private String metersAboveSeaLevel;

    @QueryParam("establishmentDate")
    private String establishmentDate;

    @QueryParam("capital")
    private String capital;

    @QueryParam("climate")
    private String climate;

    @QueryParam("coordinates.x")
    private String coordinatesX;

    @QueryParam("coordinates.y")
    private String coordinatesY;

    @QueryParam("governor.birthday")
    private String governorBirthday;

    @QueryParam("sort")
    private List<String> sort;

    @DefaultValue("1")
    @QueryParam("page")
    private String page;

    @DefaultValue("10")
    @QueryParam("size")
    private String size;
}
