package ru.itmo.soa.city.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Coordinates {
    @Column(name = "coordinate_x", nullable = false)
    private Double x;
    @Column(name = "coordinate_y", nullable = false)
    private Integer y;

    protected Coordinates() {
    }

    public Coordinates(Double x, Integer y) {
        this.x = x;
        this.y = y;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }
}
