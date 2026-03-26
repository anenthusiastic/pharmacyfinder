package com.fatih.pharmacyfinder.client;

import com.fatih.pharmacyfinder.config.PharmacyProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class NominatimClient {

    private final WebClient webClient;
    private final PharmacyProperties properties;

    public NominatimClient(WebClient webClient, PharmacyProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    @Cacheable(value = "geocoding", key = "#query")
    public Mono<List<Map<String, Object>>> geocodeAddress(String query) {
        return webClient.get()
                .uri(properties.getApi().getNominatim().getBaseUrl() + "/search?q={query}&format=json&limit=1", query)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                })
                .timeout(properties.getApi().getNominatim().getTimeout())
                .retryWhen(Retry.backoff(properties.getApi().getNominatim().getRetryAttempts(), Duration.ofSeconds(1)))
                .doOnError(error -> log.error("Geocoding failed for query: {}", query, error))
                .onErrorReturn(List.of());
    }

    public Mono<Map<String, Object>> reverseGeocode(double lat, double lon) {
        return webClient.get()
                .uri(properties.getApi().getNominatim().getBaseUrl() + "/reverse?lat={lat}&lon={lon}&format=json", lat,
                        lon)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .timeout(properties.getApi().getNominatim().getTimeout())
                .retryWhen(Retry.backoff(properties.getApi().getNominatim().getRetryAttempts(), Duration.ofSeconds(1)))
                .doOnError(error -> log.error("Reverse geocoding failed for lat={}, lon={}", lat, lon, error))
                .onErrorReturn(Map.of());
    }

    public Mono<List<Map<String, Object>>> searchPharmacies(String query, String viewbox) {
        String url = String.format("%s/search?q=%s&format=json&bounded=1&limit=%s&viewbox=%s",
                properties.getApi().getNominatim().getBaseUrl(),
                query,
                properties.getSearch().getQueryResultLimit(),
                viewbox);

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                })
                .timeout(properties.getApi().getNominatim().getTimeout())
                .retryWhen(Retry.backoff(properties.getApi().getNominatim().getRetryAttempts(), Duration.ofSeconds(1)))
                .doOnError(error -> log.error("Pharmacy search failed for query: {}", query, error))
                .onErrorReturn(List.of());
    }

    public Mono<Boolean> checkHealth() {
        return webClient.get()
                .uri(properties.getApi().getNominatim().getBaseUrl() + "/search?q=test&format=json&limit=1")
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(5))
                .map(response -> response.getStatusCode().is2xxSuccessful())
                .onErrorReturn(false);
    }
}
