package ru.itmo.soa.city.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Entity
@Table(name = "cities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class City {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column
    private String name;

    @jakarta.persistence.Embedded
    private Coordinates coordinates;

    @Column(name = "creation_date")
    private ZonedDateTime creationDate;

    @Column
    private Float area;

    @Column
    private int population;

    @Column(name = "meters_above_sea_level")
    private Double metersAboveSeaLevel;

    @Column(name = "establishment_date")
    private LocalDateTime establishmentDate;

    @Column
    private boolean capital;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private Climate climate;

    @jakarta.persistence.Embedded
    private Human governor;
}
