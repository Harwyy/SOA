package ru.itmo.soa.city.query;

import ru.itmo.soa.city.model.Climate;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

public record CityQuery(
        Integer id,
        String name,
        ZonedDateTime creationDate,
        Float area,
        Integer population,
        Double metersAboveSeaLevel,
        LocalDateTime establishmentDate,
        Boolean capital,
        Climate climate,
        Long coordinatesX,
        Float coordinatesY,
        Date governorBirthday,
        List<SortCriterion> sort,
        int page,
        int size
) {
    public CityQuery {
        sort = sort == null ? List.of() : List.copyOf(sort);
    }
}
