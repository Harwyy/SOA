package ru.itmo.soa.route;

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

public class RouteInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {
    @Override
    protected Class<?>[] getRootConfigClasses() { return null; }

    @Override
    protected Class<?>[] getServletConfigClasses() { return new Class<?>[]{RouteWebConfig.class}; }

    @Override
    protected String[] getServletMappings() { return new String[]{"/"}; }
}
