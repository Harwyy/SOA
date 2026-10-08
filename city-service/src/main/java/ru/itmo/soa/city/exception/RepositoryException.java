package ru.itmo.soa.city.exception;

public class RepositoryException extends ApplicationException {
    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
