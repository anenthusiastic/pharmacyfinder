package com.fatih.pharmacyfinder.util;

import com.fatih.pharmacyfinder.config.PharmacyProperties;
import com.fatih.pharmacyfinder.model.Pharmacy;
import com.fatih.pharmacyfinder.service.ApiClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class PharmacyFinderUtil {

    private final ApiClientService apiClientService;
    private final PharmacyProperties properties;

    public PharmacyFinderUtil(ApiClientService apiClientService, PharmacyProperties properties) {
        this.apiClientService = apiClientService;
        this.properties = properties;
    }

    public void geocodeAddress(Pharmacy pharmacy, String address, String city) {
        try {
            String query = URLEncoder.encode(address + ", " + city + ", Turkey", StandardCharsets.UTF_8);
            
            List<Map<String, Object>> results = apiClientService.geocodeAddress(query).block();
            
            if (results != null && !results.isEmpty()) {
                Map<String, Object> location = results.get(0);
                pharmacy.setLat(Double.parseDouble((String) location.get("lat")));
                pharmacy.setLon(Double.parseDouble((String) location.get("lon")));
            }
        } catch (Exception e) {
            log.error("Error geocoding address: {}", e.getMessage());
        }
    }
    
    public String generateViewbox(double userLat, double userLon, double radiusKm) {
        double deltaLat = radiusKm / properties.getSearch().getOneDegreeKm();
        double deltaLon = radiusKm / (properties.getSearch().getOneDegreeKm() * Math.cos(Math.toRadians(userLat)));
        double minLat = userLat - deltaLat;
        double maxLat = userLat + deltaLat;
        double minLon = userLon - deltaLon;
        double maxLon = userLon + deltaLon;
        return String.format("%s,%s,%s,%s", minLon, minLat, maxLon, maxLat);
    }
}
