package ru.itmo.soa.city.exception;

public class InvalidQueryException extends ApplicationException {
    public InvalidQueryException(String message) {
        super(message);
    }
}
