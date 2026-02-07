package com.fatih.pharmacyfinder.client;

import com.fatih.pharmacyfinder.config.PharmacyProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
public class OsrmClient {
    
    private final WebClient webClient;
    private final PharmacyProperties properties;
    
    public OsrmClient(WebClient webClient, PharmacyProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }
    
    public Mono<Map<String, Object>> calculateDistance(double userLon, double userLat, double pharmacyLon, double pharmacyLat) {
        String path = String.format("/route/v1/driving/%s,%s;%s,%s?overview=false", 
                                   userLon, userLat, pharmacyLon, pharmacyLat);
        
        return webClient.get()
                .uri(properties.getApi().getOsrm().getBaseUrl() + path)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(properties.getApi().getOsrm().getTimeout())
                .retryWhen(Retry.backoff(properties.getApi().getOsrm().getRetryAttempts(), Duration.ofSeconds(1)))
                .doOnError(error -> log.error("Distance calculation failed", error))
                .onErrorReturn(Map.of());
    }
    
    public Mono<Boolean> checkHealth() {
        return webClient.get()
                .uri(properties.getApi().getOsrm().getBaseUrl() + "/route/v1/driving/13.388860,52.517037;13.397634,52.529407")
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(5))
                .map(response -> response.getStatusCode().is2xxSuccessful())
                .onErrorReturn(false);
    }
}
