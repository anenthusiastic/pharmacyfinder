package com.fatih.pharmacyfinder.service;

import com.fatih.pharmacyfinder.client.NominatimClient;
import com.fatih.pharmacyfinder.exception.LocationNotDetectedException;
import com.fatih.pharmacyfinder.config.PharmacyProperties;
import com.fatih.pharmacyfinder.model.Location;
import com.fatih.pharmacyfinder.model.Pharmacy;
import com.fatih.pharmacyfinder.util.PharmacyFinderUtil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LocationService {

    private final NominatimClient nominatimClient;
    private final PharmacyProperties properties;

    public LocationService(NominatimClient nominatimClient, PharmacyProperties properties) {
        this.nominatimClient = nominatimClient;
        this.properties = properties;
    }

    public Mono<Location> enrichLocationWithAddress(Location location) {
        return nominatimClient.reverseGeocode(location.getLat(), location.getLon())
                .map(response -> {
                    if (response != null && response.containsKey("address")) {
                        Map<String, String> address = (Map<String, String>) response.get("address");
                        location.setDistrict(address.getOrDefault("town", address.getOrDefault("suburb", "")));
                        location.setCity(address.getOrDefault("province", address.getOrDefault("city", "")));
                        location.setCountryCode(
                                address.getOrDefault("country_code", properties.getDefaultCountryCode()));
                        log.info("Reverse geocoded location: {}", location);
                    }
                    return location;
                })
                .doOnError(e -> log.error("Error reverse geocoding lat={}, lon={}: {}", location.getLat(),
                        location.getLon(), e.getMessage()))
                .onErrorReturn(location);
    }

    public Mono<Pharmacy> enrichPharmacyWithCoordinates(Pharmacy pharmacy, String address, String city) {
        String query = buildQuery(city, null, address) + ", " + properties.getDefaultCountry();
        return nominatimClient.geocodeAddress(query)
                .map(results -> {
                    if (results != null && !results.isEmpty()) {
                        Map<String, Object> location = results.get(0);
                        pharmacy.setLat(Double.parseDouble((String) location.get("lat")));
                        pharmacy.setLon(Double.parseDouble((String) location.get("lon")));
                    }
                    return pharmacy;
                })
                .doOnError(e -> log.error("Error geocoding address for pharmacy {}: {}", pharmacy.getDisplayName(),
                        e.getMessage()))
                .onErrorReturn(pharmacy);
    }

    public Mono<Location> getForwardGeocode(String city, String district, String address) {
        String query = buildQuery(city, district, address);
        return nominatimClient.geocodeAddress(query)
                .map(response -> {
                    if (response != null && !response.isEmpty()) {
                        Map<String, Object> firstResult = response.get(0);
                        Location location = new Location();
                        location.setLat(Double.parseDouble((String) firstResult.get("lat")));
                        location.setLon(Double.parseDouble((String) firstResult.get("lon")));
                        log.info("Forward geocoded location: {}", location);
                        return location;
                    }
                    return new Location(); // Or empty Location
                })
                .doOnError(e -> log.error("Error forward geocoding query={}: {}", query, e.getMessage()))
                .filter(loc -> loc.getLat() != 0.0); // Filter out empty results if needed
    }

    private String buildQuery(String city, String district, String address) {
        if (PharmacyFinderUtil.areAllNullOrBlank(city, district, address)) {
            throw new LocationNotDetectedException("No location provided");
        }
        List<String> addressParts = new ArrayList<>();
        if (!PharmacyFinderUtil.isNullOrBlank(address)) {
            addressParts.add(address);
        }
        if (!PharmacyFinderUtil.isNullOrBlank(district)) {
            addressParts.add(district);
        }
        if (!PharmacyFinderUtil.isNullOrBlank(city)) {
            addressParts.add(city);
        }
        return String.join(", ", addressParts);
    }
}
