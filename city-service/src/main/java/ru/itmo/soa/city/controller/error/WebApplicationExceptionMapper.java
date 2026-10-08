package ru.itmo.soa.city.controller.error;

import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {
    @Override
    public Response toResponse(WebApplicationException exception) {
        Response original = exception.getResponse();
        int status = original == null ? 500 : original.getStatus();
        String message;
        if (exception instanceof NotAllowedException) {
            message = "HTTP-метод не поддерживается";
        } else if (exception instanceof NotAcceptableException) {
            message = "Запрошенный формат ответа не поддерживается";
        } else if (exception instanceof NotSupportedException) {
            message = "Поддерживается только application/json";
        } else {
            message = "Запрос не может быть обработан";
        }
        return ErrorResponseBuilder.create(status, message);
    }
}
