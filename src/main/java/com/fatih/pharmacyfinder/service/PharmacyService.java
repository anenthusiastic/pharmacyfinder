package com.fatih.pharmacyfinder.service;

import com.fatih.pharmacyfinder.client.NominatimClient;
import com.fatih.pharmacyfinder.client.OsrmClient;
import com.fatih.pharmacyfinder.config.PharmacyProperties;
import com.fatih.pharmacyfinder.exception.LocationNotDetectedException;
import com.fatih.pharmacyfinder.exception.NoPharmacyNearbyException;
import com.fatih.pharmacyfinder.model.Location;
import com.fatih.pharmacyfinder.model.Pharmacy;
import com.fatih.pharmacyfinder.model.PharmacyResponse;
import com.fatih.pharmacyfinder.model.PharmacySearchRequest;
import com.fatih.pharmacyfinder.util.PharmacyFinderUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PharmacyService {

    private final NominatimClient nominatimClient;
    private final OsrmClient osrmClient;
    private final LocationService locationService;
    private final PharmacyBusinessHoursService businessHoursService;
    private final OnDutyPharmacyService onDutyPharmacyService;
    private final PharmacyProperties properties;
    private final PharmacyFinderUtil pharmacyFinderUtil;
    private final ValidationService validationService;

    public PharmacyService(NominatimClient nominatimClient, OsrmClient osrmClient, LocationService locationService,
            PharmacyBusinessHoursService businessHoursService, OnDutyPharmacyService onDutyPharmacyService,
            PharmacyProperties properties, PharmacyFinderUtil pharmacyFinderUtil,
            ValidationService validationService) {
        this.nominatimClient = nominatimClient;
        this.osrmClient = osrmClient;
        this.locationService = locationService;
        this.businessHoursService = businessHoursService;
        this.onDutyPharmacyService = onDutyPharmacyService;
        this.properties = properties;
        this.pharmacyFinderUtil = pharmacyFinderUtil;
        this.validationService = validationService;
    }

    public Mono<PharmacyResponse> findPharmaciesBasedOnTime(PharmacySearchRequest request) {
        ZonedDateTime userTime = parseTime(request.getTime());

        // 1. Manuel Sehir/Ilce/Adres Kullanici Tarafindan Verilmisse
        if (!PharmacyFinderUtil.areAllNullOrBlank(request.getCity(), request.getDistrict(), request.getAddress())) {
            log.info("Finding pharmacies using manual location: {}, {}, {}", request.getCity(), request.getDistrict(),
                    request.getAddress());

            return locationService.getForwardGeocode(request.getCity(), request.getDistrict(), request.getAddress())
                    .switchIfEmpty(Mono.error(new LocationNotDetectedException(
                            "Girdiğiniz konum (şehir/ilçe/adres) doğrulanamadı. Lütfen geçerli bir yer ismi girin.")))
                    .flatMap(loc -> {
                        loc.setCity(request.getCity());
                        loc.setDistrict(request.getDistrict());
                        return processPharmacySearch(loc, userTime);
                    })
                    .map(PharmacyResponse::new);
        }

        // 2. GPS (Koordinat) Bilgisi Varsa
        if (request.getLat() != null && request.getLon() != null) {
            if (!validationService.isValidCoordinate(request.getLat(), request.getLon())) {
                String errorMessage = String.format("Invalid coordinates provided: lat=%s, lon=%s", request.getLat(),
                        request.getLon());
                log.warn(errorMessage);
                return Mono.error(new LocationNotDetectedException(errorMessage));
            }

            Location location = new Location();
            location.setLat(request.getLat());
            location.setLon(request.getLon());
            location.setCity(request.getCity());
            location.setDistrict(request.getDistrict());

            return processPharmacySearch(location, userTime)
                    .map(PharmacyResponse::new);
        }

        // 3. Eğer Ne Şehir Ne de Koordinat Yoksa Kullanıcıyı Manuel Girişe Zorla
        log.warn("No location provided (neither manual city nor GPS coordinates). Request rejected.");
        return Mono.error(new LocationNotDetectedException(
                "Konum tespit edilemedi. Lütfen GPS izni verin veya şehir/ilçe seçiminizi manuel yapın."));
    }

    private ZonedDateTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return ZonedDateTime.now();
        }
        try {
            return ZonedDateTime.parse(timeStr);
        } catch (DateTimeParseException e) {
            return ZonedDateTime.now(java.time.ZoneId.of("Europe/Istanbul"));
        }
    }

    private Mono<List<Pharmacy>> processPharmacySearch(Location location, ZonedDateTime userTime) {
        return locationService.enrichLocationWithAddress(location)
                .flatMap(loc -> businessHoursService.isPharmacyClosed(loc.getCountryCode(), userTime)
                        .flatMap(isClosed -> {
                            if (isClosed) {
                                log.info(
                                        "Out of business hours or holiday - fetching nearby on-duty pharmacies for {}, {}",
                                        loc.getDistrict(), loc.getCity());
                                return onDutyPharmacyService.getOnDutyPharmacies(loc.getDistrict(), loc.getCity())
                                        .flatMap(onDutyPharmacies -> getPharmaciesWithDistance(loc.getLat(),
                                                loc.getLon(),
                                                onDutyPharmacies));
                            } else {
                                log.info("In business hours - fetching nearby pharmacies for lat: {}, lon: {}",
                                        loc.getLat(), loc.getLon());
                                return findNearbyPharmacies(loc.getLat(), loc.getLon(),
                                        properties.getSearch().getInitialRadius());
                            }
                        }));
    }

    private Mono<List<Pharmacy>> findNearbyPharmacies(double userLat, double userLon, double radius) {
        return findNearbyPharmaciesRecursive(userLat, userLon, radius, 0);
    }

    private Mono<List<Pharmacy>> findNearbyPharmaciesRecursive(double userLat, double userLon, double radius,
            int tryCount) {
        return getPharmaciesWithinRadius(userLat, userLon, radius)
                .flatMap(pharmacies -> {
                    if (!pharmacies.isEmpty()) {
                        return getPharmaciesWithDistance(userLat, userLon, pharmacies);
                    }
                    if (tryCount < properties.getSearch().getMaxTryCount() - 1) {
                        return findNearbyPharmaciesRecursive(userLat, userLon,
                                radius + properties.getSearch().getRadiusStep(), tryCount + 1);
                    }
                    final String message = String.format(
                            "No pharmacies found in the area (lat : %s, lon : %s, radius : %s)",
                            userLat, userLon, radius);
                    log.info(message);
                    return Mono.error(new NoPharmacyNearbyException(message));
                });
    }

    private Mono<List<Pharmacy>> getPharmaciesWithDistance(double userLat, double userLon, List<Pharmacy> pharmacies) {
        return Flux.fromIterable(pharmacies)
                .flatMap(pharmacy -> calculateDistanceReactive(userLat, userLon, pharmacy))
                .collectList()
                .map(list -> list.stream()
                        .sorted(Comparator.comparingDouble(Pharmacy::getDistance))
                        .collect(Collectors.toList()));
    }

    private Mono<Pharmacy> calculateDistanceReactive(double userLat, double userLon, Pharmacy pharmacy) {
        return osrmClient.calculateDistance(userLon, userLat, pharmacy.getLon(), pharmacy.getLat())
                .map(response -> {
                    if (response.get("routes") != null) {
                        List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");
                        if (!routes.isEmpty() && routes.get(0).get("distance") != null) {
                            double distance = ((Number) routes.get(0).get("distance")).doubleValue();
                            pharmacy.setDistance(distance);
                            return pharmacy;
                        }
                    }
                    pharmacy.setDistance(Double.MAX_VALUE);
                    return pharmacy;
                })
                .doOnError(e -> log.warn("OSRM distance calculation failed for {}: {}", pharmacy.getDisplayName(),
                        e.getMessage()))
                .onErrorReturn(pharmacy.toBuilder().distance(Double.MAX_VALUE).build());
    }

    private Mono<List<Pharmacy>> getPharmaciesWithinRadius(double userLat, double userLon, double radius) {
        String viewbox = pharmacyFinderUtil.generateViewbox(userLat, userLon, radius / 1000.0);
        return nominatimClient.searchPharmacies("eczane", viewbox)
                .map(results -> {
                    if (results == null || results.isEmpty()) {
                        return List.of();
                    }
                    return results.stream()
                            .map(this::mapToPharmacy)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                });
    }

    private Pharmacy mapToPharmacy(Map<String, Object> data) {
        try {
            Pharmacy pharmacy = new Pharmacy();
            pharmacy.setDisplayName((String) data.get("display_name"));
            pharmacy.setLat(Double.parseDouble((String) data.get("lat")));
            pharmacy.setLon(Double.parseDouble((String) data.get("lon")));
            return pharmacy;
        } catch (Exception e) {
            log.warn("Failed to map pharmacy data: {}", data, e);
            return null;
        }
    }
}
