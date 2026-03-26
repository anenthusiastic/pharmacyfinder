package com.fatih.pharmacyfinder.controller;

import com.fatih.pharmacyfinder.service.PharmacyService;
import com.fatih.pharmacyfinder.service.MetricsService;
import com.fatih.pharmacyfinder.model.PharmacyResponse;
import com.fatih.pharmacyfinder.model.PharmacySearchRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/pharmacies")
public class PharmacyController {

    private final PharmacyService pharmacyService;
    private final MetricsService metricsService;

    public PharmacyController(PharmacyService pharmacyService, MetricsService metricsService) {
        this.pharmacyService = pharmacyService;
        this.metricsService = metricsService;
    }

    @GetMapping
    public Mono<ResponseEntity<PharmacyResponse>> getPharmaciesAuto(PharmacySearchRequest searchRequest) {
        metricsService.incrementPharmacyRequest();
        var timer = metricsService.startSearchTimer();

        return pharmacyService.findPharmaciesBasedOnTime(searchRequest)
                .doOnNext(response -> {
                    metricsService.recordSearchTime(timer);
                    log.info("Found {} pharmacies for request", response.getPharmacies().size());
                })
                .map(ResponseEntity::ok)
                .doOnError(e -> {
                    metricsService.recordSearchTime(timer);
                    log.error("Error processing request: {}", e.getMessage());
                });
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "pharmacy-finder"));
    }
}
