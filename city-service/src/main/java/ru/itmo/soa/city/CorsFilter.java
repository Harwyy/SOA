package ru.itmo.soa.city;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.util.Set;

@Provider
public class CorsFilter implements ContainerResponseFilter {
    private static final Set<String> ALLOWED_ORIGINS = Set.of("http://localhost:5500", "https://se.ifmo.ru");

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        String origin = request.getHeaderString("Origin");
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            response.getHeaders().putSingle("Access-Control-Allow-Origin", origin);
        }
        response.getHeaders().putSingle("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.getHeaders().putSingle("Access-Control-Allow-Headers", "Content-Type, Accept");
    }
}
