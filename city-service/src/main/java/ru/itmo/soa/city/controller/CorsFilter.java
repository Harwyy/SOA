package ru.itmo.soa.city.controller;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.container.PreMatching;

import java.io.IOException;

@Provider
@PreMatching
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final String ALLOW_METHODS = "GET, POST, PUT, DELETE, OPTIONS";
    private static final String ALLOW_HEADERS = "Accept, Content-Type";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(requestContext.getMethod())) {
            requestContext.abortWith(Response.ok().build());
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        String origin = requestContext.getHeaderString("Origin");
        responseContext.getHeaders().putSingle(
                "Access-Control-Allow-Origin", origin == null ? "*" : origin);
        responseContext.getHeaders().putSingle("Access-Control-Allow-Methods", ALLOW_METHODS);
        responseContext.getHeaders().putSingle("Access-Control-Allow-Headers", ALLOW_HEADERS);
        responseContext.getHeaders().putSingle("Access-Control-Max-Age", "3600");
        responseContext.getHeaders().putSingle("Vary", "Origin");
    }
}
