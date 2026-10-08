package ru.itmo.soa.route.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ru.itmo.soa.route.dto.CityResponse;
import ru.itmo.soa.route.exception.CityServiceUnavailableException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class RestCityClient implements CityClient {
    private static final Logger LOGGER = Logger.getLogger(RestCityClient.class.getName());

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public RestCityClient(@Value("${city.service.base-url}") String baseUrl) {
        this.baseUrl = baseUrl.replaceAll("/$", "");
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .connectTimeout(Duration.ofSeconds(5))
                        .build());
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restTemplate = new RestTemplate(requestFactory);
    }

    @Override
    public CityResponse[] findAll() {
        try {
            ResponseEntity<CityResponse[]> response = restTemplate.exchange(
                    baseUrl + "/cities", HttpMethod.GET, null, CityResponse[].class);
            return response.getBody() == null ? new CityResponse[0] : response.getBody();
        } catch (RestClientException exception) {
            LOGGER.log(Level.WARNING, "Не удалось получить города по адресу " + baseUrl + "/cities", exception);
            throw new CityServiceUnavailableException(exception);
        }
    }
}
