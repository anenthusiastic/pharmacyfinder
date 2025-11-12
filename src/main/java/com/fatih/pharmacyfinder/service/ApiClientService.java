package com.fatih.pharmacyfinder.service;

import com.fatih.pharmacyfinder.config.PharmacyProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ApiClientService {
    
    private final WebClient webClient;
    private final PharmacyProperties properties;
    
    public ApiClientService(WebClient webClient, PharmacyProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }
    
    @Cacheable(value = "geocoding", key = "#query")
    public Mono<List<Map<String, Object>>> geocodeAddress(String query) {
        return webClient.get()
                .uri(properties.getApi().getNominatim().getBaseUrl() + "/search?q={query}&format=json&limit=1", query)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .timeout(properties.getApi().getNominatim().getTimeout())
                .retryWhen(Retry.backoff(properties.getApi().getNominatim().getRetryAttempts(), Duration.ofSeconds(1))
                          .filter(this::isRetryableException))
                .doOnError(error -> log.error("Geocoding failed for query: {}", query, error))
                .onErrorReturn(List.of());
    }
    
    @Cacheable(value = "locations", key = "#ip")
    public Mono<Map<String, Object>> getLocationByIp(String ip) {
        return webClient.get()
                .uri(properties.getApi().getIpLocation().getBaseUrl() + "/json/{ip}", ip)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(properties.getApi().getIpLocation().getTimeout())
                .retryWhen(Retry.backoff(properties.getApi().getIpLocation().getRetryAttempts(), Duration.ofSeconds(1))
                          .filter(this::isRetryableException))
                .doOnError(error -> log.error("IP location lookup failed for IP: {}", ip, error))
                .onErrorReturn(Map.of());
    }
    
    public Mono<Map<String, Object>> calculateDistance(double userLon, double userLat, double pharmacyLon, double pharmacyLat) {
        String path = String.format("/route/v1/driving/%s,%s;%s,%s?overview=false", 
                                   userLon, userLat, pharmacyLon, pharmacyLat);
        
        return webClient.get()
                .uri(properties.getApi().getOsrm().getBaseUrl() + path)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(properties.getApi().getOsrm().getTimeout())
                .retryWhen(Retry.backoff(properties.getApi().getOsrm().getRetryAttempts(), Duration.ofSeconds(1))
                          .filter(this::isRetryableException))
                .doOnError(error -> log.error("Distance calculation failed", error))
                .onErrorReturn(Map.of());
    }
    
    public Mono<Map<String, Object>> fetchTurkishGovData(String district, String city) {
        return webClient.get()
                .uri(properties.getApi().getTurkishGov().getBaseUrl() + 
                     "/saglik-bakanligi-eczane-nobetci-sorgulama?ilce={district}&il={city}", district, city)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(properties.getApi().getTurkishGov().getTimeout())
                .retryWhen(Retry.backoff(properties.getApi().getTurkishGov().getRetryAttempts(), Duration.ofSeconds(1))
                          .filter(this::isRetryableException))
                .doOnError(error -> log.error("Turkish Gov API failed for district: {}, city: {}", district, city, error))
                .onErrorReturn(Map.of());
    }
    
    public Mono<List<Map<String, Object>>> searchPharmacies(String query, String viewbox) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("nominatim.openstreetmap.org")
                        .path("/search")
                        .queryParam("q", query)
                        .queryParam("format", "json")
                        .queryParam("bounded", 1)
                        .queryParam("limit", properties.getSearch().getQueryResultLimit())
                        .queryParam("viewbox", viewbox)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .timeout(properties.getApi().getNominatim().getTimeout())
                .retryWhen(Retry.backoff(properties.getApi().getNominatim().getRetryAttempts(), Duration.ofSeconds(1))
                          .filter(this::isRetryableException))
                .doOnError(error -> log.error("Pharmacy search failed for query: {}", query, error))
                .onErrorReturn(List.of());
    }
    
    private boolean isRetryableException(Throwable throwable) {
        return throwable instanceof WebClientResponseException.TooManyRequests ||
               throwable instanceof WebClientResponseException.InternalServerError ||
               throwable instanceof WebClientResponseException.BadGateway ||
               throwable instanceof WebClientResponseException.ServiceUnavailable ||
               throwable instanceof WebClientResponseException.GatewayTimeout;
    }
}