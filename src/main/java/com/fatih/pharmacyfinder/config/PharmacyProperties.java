package com.fatih.pharmacyfinder.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "pharmacy")
public class PharmacyProperties {
    
    private Api api = new Api();
    private Search search = new Search();
    private Cache cache = new Cache();
    private RateLimit rateLimit = new RateLimit();
    
    @Data
    public static class Api {
        private ApiConfig nominatim = new ApiConfig();
        private ApiConfig osrm = new ApiConfig();
        private ApiConfig turkishGov = new ApiConfig();
        private ApiConfig ipLocation = new ApiConfig();
    }
    
    @Data
    public static class ApiConfig {
        private String baseUrl;
        private Duration timeout;
        private int retryAttempts;
    }
    
    @Data
    public static class Search {
        private int radiusStep;
        private int maxTryCount;
        private int queryResultLimit;
        private int initialRadius;
        private double oneDegreeKm;
    }
    
    @Data
    public static class Cache {
        private Duration geocodingTtl;
        private Duration locationTtl;
    }
    
    @Data
    public static class RateLimit {
        private int requestsPerMinute;
    }
}