package ru.itmo.soa.city.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.List;
import ru.itmo.soa.city.repository.CityStorageException;
import ru.itmo.soa.generated.city.model.ErrorResponse;

@Provider
public class CityStorageExceptionMapper implements ExceptionMapper<CityStorageException> {
    @Override
    public Response toResponse(CityStorageException exception) {
        ErrorResponse errorResponse = new ErrorResponse(503, exception.getMessage());
        errorResponse.setDetails(List.of());
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .type("application/json")
                .entity(errorResponse)
                .build();
    }
}
