package ru.itmo.soa.city.repository;

public class CityStorageException extends RuntimeException {
    public CityStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
