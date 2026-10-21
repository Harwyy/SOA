package ru.itmo.soa.city.filter;

import jakarta.json.Json;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import ru.itmo.soa.city.exception.CityException;
import jakarta.ws.rs.core.Response;

@Provider
public class CityRequestTypeFilter implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (!isCityWriteRequest(requestContext)) {
            return;
        }

        byte[] requestBody = requestContext.getEntityStream().readAllBytes();
        requestContext.setEntityStream(new ByteArrayInputStream(requestBody));
        if (requestBody.length == 0) {
            return;
        }

        try (JsonReader jsonReader = Json.createReader(new ByteArrayInputStream(requestBody))) {
            JsonObject cityRequest = jsonReader.readObject();
            requireInteger(cityRequest, "area");
            requireInteger(cityRequest, "population");
            JsonValue coordinatesValue = cityRequest.get("coordinates");
            if (coordinatesValue instanceof JsonObject coordinates) {
                requireInteger(coordinates, "y");
            }
        }
    }

    private boolean isCityWriteRequest(ContainerRequestContext requestContext) {
        String method = requestContext.getMethod();
        String path = requestContext.getUriInfo().getPath();
        MediaType mediaType = requestContext.getMediaType();
        return ("POST".equals(method) || "PUT".equals(method))
                && path.startsWith("cities")
                && mediaType != null
                && mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE);
    }

    private void requireInteger(JsonObject object, String fieldName) {
        JsonValue value = object.get(fieldName);
        if (value instanceof JsonNumber number && !number.isIntegral()) {
            throw new CityException(fieldName + " должен быть целым числом", Response.Status.BAD_REQUEST);
        }
    }
}
