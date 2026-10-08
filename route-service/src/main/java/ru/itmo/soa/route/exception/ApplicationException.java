package ru.itmo.soa.route.exception;

public abstract class ApplicationException extends RuntimeException {
    protected ApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
