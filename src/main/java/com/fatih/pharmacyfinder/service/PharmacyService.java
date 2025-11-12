package com.fatih.pharmacyfinder.service;

import com.fatih.pharmacyfinder.config.PharmacyProperties;
import com.fatih.pharmacyfinder.exception.LocationNotDetectedException;
import com.fatih.pharmacyfinder.exception.NoPharmacyNearbyException;
import com.fatih.pharmacyfinder.model.Pharmacy;
import com.fatih.pharmacyfinder.util.PharmacyFinderUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PharmacyService implements IPharmacyService{

    private final ApiClientService apiClientService;
    private final LocationService locationService;
    private final PharmacyScheduleService scheduleService;
    private final OnDutyPharmacyService onDutyPharmacyService;
    private final PharmacyProperties properties;
    private final PharmacyFinderUtil pharmacyFinderUtil;
    private final ValidationService validationService;

    public PharmacyService(ApiClientService apiClientService, LocationService locationService, 
                           PharmacyScheduleService scheduleService, OnDutyPharmacyService onDutyPharmacyService,
                           PharmacyProperties properties, PharmacyFinderUtil pharmacyFinderUtil,
                           ValidationService validationService) {
        this.apiClientService = apiClientService;
        this.locationService = locationService;
        this.scheduleService = scheduleService;
        this.onDutyPharmacyService = onDutyPharmacyService;
        this.properties = properties;
        this.pharmacyFinderUtil = pharmacyFinderUtil;
        this.validationService = validationService;
    }

    @Override
    public List<Pharmacy> findPharmaciesBasedOnTime(Double lat, Double lon, String ipAddress) {
        if (validationService.isValidCoordinate(lat, lon)) {
            var location = locationService.getReverseGeocode(lat, lon);
            if (location == null) {
                log.error("Could not reverse geocode location. Falling back to IP-based location.");
                return findPharmaciesBasedOnTimeByIp(ipAddress);
            }
            return processPharmacySearch(location);
        }
        return findPharmaciesBasedOnTimeByIp(ipAddress);
    }

    private List<Pharmacy> findPharmaciesBasedOnTimeByIp(String ipAddress) {
        if(!validationService.isValidIpAddress(ipAddress)) {
            String errorMessage = "Invalid IP address provided. Cannot determine location.";
            log.error(errorMessage);
            throw new LocationNotDetectedException(errorMessage);
        }
        
        String sanitizedIp = validationService.sanitizeInput(ipAddress);
        var location = locationService.detectUserLocation(sanitizedIp);
        if (location == null) {
            String errorMessage = "Could not detect user location from IP";
            log.error(errorMessage);
            throw new LocationNotDetectedException(errorMessage);
        }
        return processPharmacySearch(location);
    }

    private List<Pharmacy> processPharmacySearch(com.fatih.pharmacyfinder.model.Location location) {
        var closingTime = scheduleService.getClosingTime();

        if (scheduleService.isAfterClosingTime(closingTime)) {
            log.info("Out of business hours - fetching on-duty pharmacies");
            return onDutyPharmacyService.getOnDutyPharmacies(location.getDistrict(), location.getCity());
        } else {
            log.info("In business hours - fetching nearby pharmacies");
            return findNearbyPharmacies(location.getLat(), location.getLon(), properties.getSearch().getInitialRadius());
        }
    }

    private List<Pharmacy> findNearbyPharmacies(double userLat, double userLon, double radius) {
        List<Pharmacy> pharmacies;
        int tryCount = 0;
        do {
            try {
                pharmacies = getPharmaciesWithinRadius(userLat, userLon, radius);
            } catch (Exception e) {
                final String message = "Error fetching pharmacies from Nominatim API";
                log.error(message + ": {}", e.getMessage());
                throw new RuntimeException(message, e);
            }
            if (!pharmacies.isEmpty()) break;
            radius += properties.getSearch().getRadiusStep();
            tryCount++;
        } while (tryCount < properties.getSearch().getMaxTryCount());

        if (pharmacies.isEmpty()) {
            final String message = String.format("No pharmacies found in the area (lat : %s, lon : %s, radius : %s)", userLat, userLon, radius);
            log.info(message);
            throw new NoPharmacyNearbyException(message);
        }

        // Use reactive approach for distance calculations
        List<Pharmacy> pharmaciesWithDistance = Flux.fromIterable(pharmacies)
                .flatMap(pharmacy -> calculateDistanceReactive(userLat, userLon, pharmacy))
                .collectList()
                .block();

        return pharmaciesWithDistance.stream()
                .sorted(Comparator.comparingDouble(Pharmacy::getDistance))
                .collect(Collectors.toList());
    }

    private Mono<Pharmacy> calculateDistanceReactive(double userLat, double userLon, Pharmacy pharmacy) {
        return apiClientService.calculateDistance(userLon, userLat, pharmacy.getLon(), pharmacy.getLat())
                .map(response -> {
                    if (response.get("routes") != null) {
                        List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");
                        if (!routes.isEmpty() && routes.get(0).get("distance") != null) {
                            double distance = ((Number) routes.get(0).get("distance")).doubleValue();
                            pharmacy.setDistance(distance);
                            return pharmacy;
                        }
                    }
                    log.warn("Invalid OSRM response for pharmacy: {}", pharmacy.getDisplay_name());
                    pharmacy.setDistance(Double.MAX_VALUE);
                    return pharmacy;
                })
                .onErrorReturn(pharmacy.toBuilder().distance(Double.MAX_VALUE).build());
    }

    private List<Pharmacy> getPharmaciesWithinRadius(double userLat, double userLon, double radius) {
        String viewbox = pharmacyFinderUtil.generateViewbox(userLat, userLon, radius / 1000.0);
        
        List<Map<String, Object>> results = apiClientService.searchPharmacies("eczane", viewbox).block();
        
        if (results == null || results.isEmpty()) {
            return List.of();
        }
        
        return results.stream()
                .map(this::mapToPharmacy)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    private Pharmacy mapToPharmacy(Map<String, Object> data) {
        try {
            Pharmacy pharmacy = new Pharmacy();
            pharmacy.setDisplay_name((String) data.get("display_name"));
            pharmacy.setLat(Double.parseDouble((String) data.get("lat")));
            pharmacy.setLon(Double.parseDouble((String) data.get("lon")));
            return pharmacy;
        } catch (Exception e) {
            log.warn("Failed to map pharmacy data: {}", data, e);
            return null;
        }
    }


}
