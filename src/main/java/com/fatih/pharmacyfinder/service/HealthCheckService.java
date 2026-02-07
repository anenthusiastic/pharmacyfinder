package com.fatih.pharmacyfinder.service;

import com.fatih.pharmacyfinder.client.NominatimClient;
import com.fatih.pharmacyfinder.client.OsrmClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class HealthCheckService {
    
    private final NominatimClient nominatimClient;
    private final OsrmClient osrmClient;
    
    public HealthCheckService(NominatimClient nominatimClient, OsrmClient osrmClient) {
        this.nominatimClient = nominatimClient;
        this.osrmClient = osrmClient;
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
            return Boolean.TRUE.equals(nominatimClient.checkHealth().block());
        } catch (Exception e) {
            log.warn("Nominatim health check failed", e);
            return false;
        }
    }
    
    private boolean checkOsrmHealth() {
        try {
            return Boolean.TRUE.equals(osrmClient.checkHealth().block());
        } catch (Exception e) {
            log.warn("OSRM health check failed", e);
            return false;
        }
    }
}