package ru.itmo.soa.city.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.itmo.soa.city.model.Climate;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class CityRequest {
    @NotBlank
    private String name;

    @NotNull
    @Valid
    private CoordinatesDto coordinates;

    @NotNull
    @Positive
    private Float area;

    @Positive
    private int population;
    private Double metersAboveSeaLevel;
    private LocalDateTime establishmentDate;
    private boolean capital;
    private Climate climate;

    @Valid
    private HumanDto governor;
}
