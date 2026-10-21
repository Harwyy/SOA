package ru.itmo.soa.city.exception;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.List;
import ru.itmo.soa.generated.city.model.ErrorResponse;

@Provider
public class RequestProcessingExceptionMapper implements ExceptionMapper<ProcessingException> {
    @Override
    public Response toResponse(ProcessingException exception) {
        ErrorResponse errorResponse = new ErrorResponse(400, "Некорректные данные запроса");
        errorResponse.setDetails(List.of());
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorResponse)
                .type("application/json")
                .build();
    }
}
