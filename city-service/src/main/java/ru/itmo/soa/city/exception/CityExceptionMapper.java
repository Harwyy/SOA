package ru.itmo.soa.city.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.List;
import ru.itmo.soa.generated.city.model.ErrorResponse;

@Provider
public class CityExceptionMapper implements ExceptionMapper<CityException> {
    @Override
    public Response toResponse(CityException exception) {
        ErrorResponse errorResponse = new ErrorResponse(
                exception.getStatus().getStatusCode(), exception.getMessage());
        errorResponse.setDetails(List.of());
        return Response.status(exception.getStatus())
                .entity(errorResponse)
                .type("application/json")
                .build();
    }
}
