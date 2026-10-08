package ru.itmo.soa.city.controller.error;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import ru.itmo.soa.city.exception.ApplicationException;
import ru.itmo.soa.city.exception.CityNotFoundException;
import ru.itmo.soa.city.exception.ConflictException;
import ru.itmo.soa.city.exception.RepositoryException;

import java.util.List;

@Provider
public class ApplicationExceptionMapper implements ExceptionMapper<ApplicationException> {
    @Override
    public Response toResponse(ApplicationException exception) {
        return ErrorResponseBuilder.create(
                statusOf(exception),
                publicMessage(exception),
                List.of());
    }

    private int statusOf(ApplicationException exception) {
        if (exception instanceof CityNotFoundException) {
            return Response.Status.NOT_FOUND.getStatusCode();
        }
        if (exception instanceof RepositoryException) {
            return Response.Status.SERVICE_UNAVAILABLE.getStatusCode();
        }
        if (exception instanceof ConflictException) {
            return Response.Status.CONFLICT.getStatusCode();
        }
        return Response.Status.BAD_REQUEST.getStatusCode();
    }

    private String publicMessage(ApplicationException exception) {
        if (exception instanceof RepositoryException) {
            return "Хранилище городов недоступно";
        }
        if (exception instanceof ConflictException) {
            return "Нарушено ограничение целостности данных";
        }
        return exception.getMessage();
    }

}
