package ru.itmo.soa.city.exception;

import jakarta.ws.rs.core.Response;

public class CityException extends RuntimeException {
    private final Response.Status status;

    public CityException(String message, Response.Status status) {
        super(message);
        this.status = status;
    }

    public CityException(String message, Response.Status status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public Response.Status getStatus() {
        return status;
    }
}
