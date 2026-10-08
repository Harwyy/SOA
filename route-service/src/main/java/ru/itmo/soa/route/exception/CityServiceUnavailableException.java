package ru.itmo.soa.route.exception;

public class CityServiceUnavailableException extends ApplicationException {
    public CityServiceUnavailableException(Throwable cause) {
        super("Первый сервис городов недоступен", cause);
    }
}
