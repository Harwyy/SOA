package ru.itmo.soa.city.time;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import ru.itmo.soa.city.exception.CityException;
import jakarta.ws.rs.core.Response;

/** Russian user-facing date-time format used by the public API. */
public final class CityDateTime {
    public static final String PATTERN = "dd.MM.uuuu HH:mm";
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(PATTERN)
            .withResolverStyle(ResolverStyle.STRICT);

    private CityDateTime() {
    }

    public static LocalDateTime parse(String value, String fieldName) {
        try {
            return LocalDateTime.parse(value, FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new CityException(
                    fieldName + " должен иметь формат ДД.ММ.ГГГГ ЧЧ:ММ",
                    Response.Status.BAD_REQUEST,
                    exception);
        }
    }

    public static String format(LocalDateTime value) {
        return value == null ? null : FORMATTER.format(value);
    }
}
