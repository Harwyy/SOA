package ru.itmo.soa.city.query;

import ru.itmo.soa.city.model.Climate;
import ru.itmo.soa.city.dto.CitySearchParams;
import ru.itmo.soa.city.exception.InvalidQueryException;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class CityQueryParser {
    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "id", "name", "creationDate", "area", "population", "metersAboveSeaLevel",
            "establishmentDate", "capital", "climate", "coordinates.x", "coordinates.y",
            "governor.birthday");

    public CityQuery parse(CitySearchParams params) {
        int parsedPage = parseInt(params.getPage(), "page", 1);
        int parsedSize = parseInt(params.getSize(), "size", 1);
        if (parsedSize > 100) {
            throw invalid("Параметр 'size' должен быть не больше 100");
        }
        return new CityQuery(
                optionalInt(params.getId(), "id", 1),
                params.getName(),
                optionalZonedDateTime(params.getCreationDate(), "creationDate"),
                optionalFloat(params.getArea(), "area", true),
                optionalInt(params.getPopulation(), "population", 1),
                optionalDouble(params.getMetersAboveSeaLevel(), "metersAboveSeaLevel"),
                optionalLocalDateTime(params.getEstablishmentDate(), "establishmentDate"),
                optionalBoolean(params.getCapital(), "capital"),
                optionalClimate(params.getClimate()),
                optionalLong(params.getCoordinatesX(), "coordinates.x"),
                optionalFloat(params.getCoordinatesY(), "coordinates.y", false),
                optionalDate(params.getGovernorBirthday(), "governor.birthday"),
                parseSort(params.getSort()),
                parsedPage,
                parsedSize);
    }

    private List<SortCriterion> parseSort(List<String> expressions) {
        if (expressions == null) {
            return List.of();
        }
        return expressions.stream()
                .map(this::parseSortExpression)
                .toList();
    }

    private SortCriterion parseSortExpression(String expression) {
        String[] parts = expression.split(",", -1);
        if (parts.length != 2
                || !SORTABLE_FIELDS.contains(parts[0])
                || !(parts[1].equals("asc") || parts[1].equals("desc"))) {
            throw invalid("Некорректный параметр sort: " + expression);
        }
        return new SortCriterion(parts[0], parts[1].equals("desc"));
    }

    public int parseRequiredId(String value) {
        return parseInt(value, "id", 1);
    }

    public double parseRequiredDouble(String value, String name) {
        if (value == null || value.isBlank()) throw invalid("Параметр '" + name + "' обязателен");
        return optionalDouble(value, name);
    }

    public LocalDateTime parseRequiredLocalDateTime(String value, String name) {
        if (value == null || value.isBlank()) throw invalid("Параметр '" + name + "' обязателен");
        return optionalLocalDateTime(value, name);
    }

    private Integer optionalInt(String value, String name, int minimum) {
        return value == null ? null : parseInt(value, name, minimum);
    }

    private int parseInt(String value, String name, int minimum) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < minimum) throw invalid("Параметр '" + name + "' должен быть не меньше " + minimum);
            return parsed;
        } catch (NumberFormatException e) {
            throw invalid("Параметр '" + name + "' должен быть целым числом");
        }
    }

    private Long optionalLong(String value, String name) {
        if (value == null) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw invalid("Параметр '" + name + "' должен быть целым числом");
        }
    }

    private Float optionalFloat(String value, String name, boolean positive) {
        if (value == null) return null;
        try {
            float parsed = Float.parseFloat(value);
            if (!Float.isFinite(parsed) || (positive && parsed <= 0) || (!positive && parsed > 952)) {
                throw invalid("Некорректное значение параметра '" + name + "'");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw invalid("Параметр '" + name + "' должен быть числом");
        }
    }

    private Double optionalDouble(String value, String name) {
        if (value == null) return null;
        try {
            double parsed = Double.parseDouble(value);
            if (!Double.isFinite(parsed)) throw invalid("Некорректное значение параметра '" + name + "'");
            return parsed;
        } catch (NumberFormatException e) {
            throw invalid("Параметр '" + name + "' должен быть числом");
        }
    }

    private Boolean optionalBoolean(String value, String name) {
        if (value == null) return null;
        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
            throw invalid("Параметр '" + name + "' должен быть true или false");
        }
        return Boolean.parseBoolean(value);
    }

    private Climate optionalClimate(String value) {
        if (value == null) return null;
        try {
            return Climate.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw invalid("Неизвестное значение climate: " + value);
        }
    }

    private ZonedDateTime optionalZonedDateTime(String value, String name) {
        if (value == null) return null;
        try {
            return ZonedDateTime.parse(value);
        } catch (DateTimeParseException e) {
            throw invalid("Параметр '" + name + "' должен быть в формате date-time");
        }
    }

    private LocalDateTime optionalLocalDateTime(String value, String name) {
        if (value == null) return null;
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            throw invalid("Параметр '" + name + "' должен быть в формате local date-time");
        }
    }

    private Date optionalDate(String value, String name) {
        if (value == null) return null;
        try {
            return Date.from(ZonedDateTime.parse(value).toInstant());
        } catch (DateTimeParseException e) {
            throw invalid("Параметр '" + name + "' должен быть в формате date-time");
        }
    }

    private InvalidQueryException invalid(String message) {
        return new InvalidQueryException(message);
    }
}
