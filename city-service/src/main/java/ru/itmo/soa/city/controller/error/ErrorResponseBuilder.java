package ru.itmo.soa.city.controller.error;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ru.itmo.soa.city.dto.error.ErrorResponse;

import java.util.List;

public final class ErrorResponseBuilder {
    private ErrorResponseBuilder() {
    }

    public static Response create(int status, String message) {
        return create(status, message, List.of());
    }

    public static Response create(int status, String message, List<String> details) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse(status, message, details))
                .build();
    }
}
