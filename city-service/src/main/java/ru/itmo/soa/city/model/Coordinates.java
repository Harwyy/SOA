package ru.itmo.soa.city.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class Coordinates {
    @Column(name = "coordinates_x")
    private Long x;

    @Column(name = "coordinates_y")
    private float y;
}
