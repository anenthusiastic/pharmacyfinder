package com.fatih.pharmacyfinder.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MetricsService {
    
    private final Counter pharmacyRequestCounter;
    private final MeterRegistry meterRegistry;
    private final Timer pharmacySearchTimer;
    private final Counter rateLimitCounter;
    
    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.pharmacyRequestCounter = Counter.builder("pharmacy.requests.total")
                .description("Total number of pharmacy requests")
                .register(meterRegistry);
                
        this.pharmacySearchTimer = Timer.builder("pharmacy.search.duration")
                .description("Time taken to search pharmacies")
                .register(meterRegistry);
                
        this.rateLimitCounter = Counter.builder("pharmacy.rate.limit.exceeded")
                .description("Number of rate limit violations")
                .register(meterRegistry);
    }
    
    public void incrementPharmacyRequest() {
        pharmacyRequestCounter.increment();
    }
    
    public void incrementApiError(String apiName) {
        Counter.builder("pharmacy.api.errors.total")
                .description("Total number of API errors")
                .tag("api", apiName)
                .register(meterRegistry)
                .increment();
    }
    
    public Timer.Sample startSearchTimer() {
        return Timer.start();
    }
    
    public void recordSearchTime(Timer.Sample sample) {
        sample.stop(pharmacySearchTimer);
    }
    
    public void incrementRateLimitExceeded() {
        rateLimitCounter.increment();
    }
}