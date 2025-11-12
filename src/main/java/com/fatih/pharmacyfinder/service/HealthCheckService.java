package com.fatih.pharmacyfinder.service;

import com.fatih.pharmacyfinder.config.PharmacyProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class HealthCheckService {
    
    private final WebClient webClient;
    private final PharmacyProperties properties;
    
    public HealthCheckService(WebClient webClient, PharmacyProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }
    
    public Map<String, Object> checkHealth() {
        Map<String, Object> health = new HashMap<>();
        boolean nominatimHealthy = checkNominatimHealth();
        boolean osrmHealthy = checkOsrmHealth();
        
        health.put("status", (nominatimHealthy && osrmHealthy) ? "UP" : "DOWN");
        health.put("nominatim", nominatimHealthy ? "UP" : "DOWN");
        health.put("osrm", osrmHealthy ? "UP" : "DOWN");
        
        return health;
    }
    
    private boolean checkNominatimHealth() {
        try {
            return webClient.get()
                    .uri(properties.getApi().getNominatim().getBaseUrl() + "/search?q=test&format=json&limit=1")
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(5))
                    .map(response -> response.getStatusCode().is2xxSuccessful())
                    .onErrorReturn(false)
                    .block();
        } catch (Exception e) {
            log.warn("Nominatim health check failed", e);
            return false;
        }
    }
    
    private boolean checkOsrmHealth() {
        try {
            return webClient.get()
                    .uri(properties.getApi().getOsrm().getBaseUrl() + "/route/v1/driving/13.388860,52.517037;13.397634,52.529407")
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(5))
                    .map(response -> response.getStatusCode().is2xxSuccessful())
                    .onErrorReturn(false)
                    .block();
        } catch (Exception e) {
            log.warn("OSRM health check failed", e);
            return false;
        }
    }
}