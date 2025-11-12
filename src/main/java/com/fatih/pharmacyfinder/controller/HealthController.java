package com.fatih.pharmacyfinder.controller;

import com.fatih.pharmacyfinder.service.HealthCheckService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {
    
    private final HealthCheckService healthCheckService;
    
    public HealthController(HealthCheckService healthCheckService) {
        this.healthCheckService = healthCheckService;
    }
    
    @GetMapping("/health")
    public Map<String, Object> health() {
        return healthCheckService.checkHealth();
    }
}
