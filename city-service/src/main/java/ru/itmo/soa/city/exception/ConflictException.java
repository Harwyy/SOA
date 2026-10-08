package ru.itmo.soa.city.exception;

public class ConflictException extends ApplicationException {
    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
