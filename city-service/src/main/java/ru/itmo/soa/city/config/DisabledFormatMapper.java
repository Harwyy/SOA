package ru.itmo.soa.city.config;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.FormatMapper;

/**
 * Prevents Hibernate from auto-detecting incompatible server-provided
 * Jackson/JAXB implementations. The domain model does not use JSON/XML JDBC
 * mappings, so these methods must never be called.
 */
public final class DisabledFormatMapper implements FormatMapper {
    @Override
    public <T> T fromString(CharSequence value, JavaType<T> javaType, WrapperOptions options) {
        throw new UnsupportedOperationException("JSON/XML format mapping is disabled");
    }

    @Override
    public <T> String toString(T value, JavaType<T> javaType, WrapperOptions options) {
        throw new UnsupportedOperationException("JSON/XML format mapping is disabled");
    }
}
