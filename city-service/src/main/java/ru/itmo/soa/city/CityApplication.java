package ru.itmo.soa.city;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.Set;
import ru.itmo.soa.city.controller.CityController;
import ru.itmo.soa.city.exception.CityExceptionMapper;
import ru.itmo.soa.city.exception.RequestProcessingExceptionMapper;
import ru.itmo.soa.city.exception.ValidationExceptionMapper;
import ru.itmo.soa.city.filter.CityRequestTypeFilter;

@ApplicationPath("/")
public class CityApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(
                CityController.class,
                CityExceptionMapper.class,
                RequestProcessingExceptionMapper.class,
                ValidationExceptionMapper.class,
                CityRequestTypeFilter.class,
                CorsFilter.class);
    }
}
