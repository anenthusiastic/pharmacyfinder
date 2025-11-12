package com.fatih.pharmacyfinder.service;

import com.fatih.pharmacyfinder.model.Location;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
public class LocationService {

    private final RestTemplate restTemplate;

    public LocationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Location detectUserLocation(String ipAddress) {
        try {
            String url = String.format("http://ip-api.com/json/%s?fields=lat,lon,city,district", ipAddress);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null) {
                Location location = new Location();
                location.setLat(((Number) response.get("lat")).doubleValue());
                location.setLon(((Number) response.get("lon")).doubleValue());
                location.setCity((String) response.get("city"));
                location.setDistrict((String) response.getOrDefault("district", ""));
                log.info("Detected location: {}", location);
                return location;
            }
        } catch (Exception e) {
            log.error("Error detecting location for IP {}: {}", ipAddress, e.getMessage());
        }
        return null;
    }

    public Location getReverseGeocode(double lat, double lon) {
        try {
            String url = String.format("https://nominatim.openstreetmap.org/reverse?lat=%s&lon=%s&format=json", lat, lon);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && response.containsKey("address")) {
                Map<String, String> address = (Map<String, String>) response.get("address");
                Location location = new Location();
                location.setLat(lat);
                location.setLon(lon);
                location.setDistrict(address.getOrDefault("suburb", address.getOrDefault("town", "")));
                location.setCity(address.getOrDefault("city", address.getOrDefault("province", "")));
                log.info("Reverse geocoded location: {}", location);
                return location;
            }
        } catch (Exception e) {
            log.error("Error reverse geocoding lat={}, lon={}: {}", lat, lon, e.getMessage());
        }
        return null;
    }
}
