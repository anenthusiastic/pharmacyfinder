package com.fatih.pharmacyfinder.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitConfig {
    
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    
    @Bean
    public ConcurrentHashMap<String, Bucket> rateLimitBuckets() {
        return buckets;
    }
    
    public Bucket createBucket(PharmacyProperties properties) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(properties.getRateLimit().getRequestsPerMinute(), 
                         Refill.intervally(properties.getRateLimit().getRequestsPerMinute(), Duration.ofMinutes(1))))
                .build();
    }
}