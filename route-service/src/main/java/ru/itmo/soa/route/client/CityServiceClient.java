package ru.itmo.soa.route.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import ru.itmo.soa.route.exception.UpstreamServiceException;

@Component
public class CityServiceClient {
    private static final int PAGE_SIZE = 100;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String cityServiceBaseUrl;

    public CityServiceClient() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(), new ObjectMapper(),
                System.getProperty("city.service.base-url", "https://localhost:8181/city-service"));
    }

    CityServiceClient(HttpClient httpClient, ObjectMapper objectMapper, String cityServiceBaseUrl) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.cityServiceBaseUrl = cityServiceBaseUrl;
    }

    public List<JsonNode> getAllCities() {
        List<JsonNode> cities = new ArrayList<>();
        for (int page = 1; ; page++) {
            JsonNode cityPage = requestCityPage(page);
            if (cityPage == null) {
                break;
            }
            cityPage.forEach(cities::add);
            if (cityPage.size() < PAGE_SIZE) {
                break;
            }
        }
        return cities;
    }

    private JsonNode requestCityPage(int page) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(
                        cityServiceBaseUrl + "/cities?page=" + page + "&size=" + PAGE_SIZE))
                .header("Accept", "application/json")
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 204) {
                return null;
            }
            if (response.statusCode() != 200) {
                throw new UpstreamServiceException(
                        "Первый сервис вернул HTTP " + response.statusCode());
            }
            JsonNode cityPage = objectMapper.readTree(response.body());
            if (!cityPage.isArray()) {
                throw new UpstreamServiceException("Первый сервис вернул некорректный ответ");
            }
            return cityPage;
        } catch (IOException exception) {
            throw new UpstreamServiceException("Не удалось получить данные первого сервиса", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new UpstreamServiceException("Не удалось получить данные первого сервиса", exception);
        }
    }
}
