package com.fatih.pharmacyfinder.service;

import com.fatih.pharmacyfinder.client.IpLocationClient;
import com.fatih.pharmacyfinder.client.NominatimClient;
import com.fatih.pharmacyfinder.model.Location;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class LocationService {

    private final IpLocationClient ipLocationClient;
    private final NominatimClient nominatimClient;

    public LocationService(IpLocationClient ipLocationClient, NominatimClient nominatimClient) {
        this.ipLocationClient = ipLocationClient;
        this.nominatimClient = nominatimClient;
    }

    public Location detectUserLocation(String ipAddress) {
        try {
            Map<String, Object> response = ipLocationClient.getLocationByIp(ipAddress).block();
            
            if (response != null && !response.isEmpty()) {
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
            Map<String, Object> response = nominatimClient.reverseGeocode(lat, lon).block();
            
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
