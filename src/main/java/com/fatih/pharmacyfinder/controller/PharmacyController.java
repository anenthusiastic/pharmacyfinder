package com.fatih.pharmacyfinder.controller;

import com.fatih.pharmacyfinder.model.Pharmacy;
import com.fatih.pharmacyfinder.service.IPharmacyService;
import com.fatih.pharmacyfinder.service.MetricsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/pharmacies")
public class PharmacyController {

    private final IPharmacyService pharmacyService;
    private final MetricsService metricsService;

    public PharmacyController(IPharmacyService pharmacyService, MetricsService metricsService) {
        this.pharmacyService = pharmacyService;
        this.metricsService = metricsService;
    }

    @GetMapping
    public ResponseEntity<?> getPharmaciesAuto(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            HttpServletRequest request) {
        
        metricsService.incrementPharmacyRequest();
        var timer = metricsService.startSearchTimer();
        
        String ipAddress = getClientIp(request);
        try {
            List<Pharmacy> pharmacies = pharmacyService.findPharmaciesBasedOnTime(lat, lon, ipAddress);
            metricsService.recordSearchTime(timer);
            log.info("Found {} pharmacies for request from IP: {}", pharmacies.size(), ipAddress);
            return ResponseEntity.ok(pharmacies);
        } catch (Exception e) {
            metricsService.recordSearchTime(timer);
            log.error("Error processing pharmacy request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unable to fetch pharmacies at this time"));
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "pharmacy-finder"));
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip != null ? ip.split(",")[0].trim() : "unknown";
    }
}
