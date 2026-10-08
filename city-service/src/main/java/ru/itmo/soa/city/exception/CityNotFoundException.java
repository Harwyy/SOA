package ru.itmo.soa.city.exception;

public class CityNotFoundException extends ApplicationException {
    public CityNotFoundException(String message) {
        super(message);
    }
}
