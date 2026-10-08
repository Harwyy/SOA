package ru.itmo.soa.city.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.itmo.soa.city.model.Climate;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CityResponse {
    private int id;
    private String name;
    private CoordinatesDto coordinates;
    private ZonedDateTime creationDate;
    private Float area;
    private int population;
    private Double metersAboveSeaLevel;
    private LocalDateTime establishmentDate;
    private boolean capital;
    private Climate climate;
    private HumanDto governor;
}
