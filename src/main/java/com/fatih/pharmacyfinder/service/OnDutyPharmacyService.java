package com.fatih.pharmacyfinder.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fatih.pharmacyfinder.client.CollectApiClient;
import com.fatih.pharmacyfinder.client.IzmirOpenDataClient;
import com.fatih.pharmacyfinder.client.TurkishGovClient;
import com.fatih.pharmacyfinder.model.Pharmacy;
import com.fatih.pharmacyfinder.util.PharmacyFinderUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OnDutyPharmacyService {

    private final TurkishGovClient turkishGovClient;
    private final OnDutyPharmacyScraperService scraperService;
    private final ValidationService validationService;
    private final IzmirOpenDataClient izmirClient;
    private final CollectApiClient collectApiClient;
    private final LocationService locationService;

    public OnDutyPharmacyService(TurkishGovClient turkishGovClient,
            OnDutyPharmacyScraperService scraperService,
            ValidationService validationService,
            IzmirOpenDataClient izmirClient,
            CollectApiClient collectApiClient,
            LocationService locationService) {
        this.turkishGovClient = turkishGovClient;
        this.scraperService = scraperService;
        this.validationService = validationService;
        this.izmirClient = izmirClient;
        this.collectApiClient = collectApiClient;
        this.locationService = locationService;
    }

    @Cacheable(value = "pharmacies", key = "#district + '_' + #city")
    public Mono<List<Pharmacy>> getOnDutyPharmacies(String district, String city) {
        if (!validationService.isValidCityOrDistrict(district) || !validationService.isValidCityOrDistrict(city)) {
            log.warn("Invalid district or city provided: {}, {}", district, city);
            return Mono.just(List.of());
        }

        String sanitizedDistrict = validationService.sanitizeInput(district);
        String sanitizedCity = validationService.sanitizeInput(city);

        return fetchFromIzmirIfApplicable(sanitizedCity, sanitizedDistrict)
                .flatMap(list -> {
                    if (!list.isEmpty())
                        return Mono.just(list);
                    return fetchFromCollectApi(sanitizedDistrict, sanitizedCity);
                })
                .flatMap(list -> {
                    if (!list.isEmpty())
                        return Mono.just(list);
                    return fetchFromGovApi(sanitizedDistrict, sanitizedCity);
                })
                .flatMap(list -> {
                    if (!list.isEmpty())
                        return Mono.just(list);
                    return scraperService.scrapeOnDutyPharmacies(sanitizedDistrict, sanitizedCity);
                });
    }

    private Mono<List<Pharmacy>> fetchFromIzmirIfApplicable(String city, String district) {
        if (city.equalsIgnoreCase("izmir") || city.equalsIgnoreCase("İzmir")) {
            log.info("Using Izmir Open Data API for {}", district);
            return fetchFromIzmirApi(district);
        }
        return Mono.just(List.of());
    }

    private Mono<List<Pharmacy>> fetchFromIzmirApi(String district) {
        return izmirClient.fetchOnDutyPharmacies()
                .map(root -> {
                    List<Pharmacy> pharmacies = new ArrayList<>();
                    if (root != null && root.isArray()) {
                        log.info("Izmir API returned {} items total", root.size());
                        int loggedCount = 0;
                        for (JsonNode node : root) {
                            String ilce = node.path("Ilce").asText();
                            if (loggedCount < 5) {
                                log.info("Sample district name from API: '{}'", ilce);
                                loggedCount++;
                            }
                            if (district.equalsIgnoreCase(ilce.trim())) {
                                Pharmacy p = new Pharmacy();
                                p.setDisplayName(node.path("Adi").asText());
                                p.setLat(node.path("LokasyonY").asDouble());
                                p.setLon(node.path("LokasyonX").asDouble());
                                pharmacies.add(p);
                            }
                        }
                    }
                    log.info("Izmir API match result for '{}': {} pharmacies found", district, pharmacies.size());
                    return pharmacies;
                })
                .onErrorReturn(List.of());
    }

    private Mono<List<Pharmacy>> fetchFromCollectApi(String district, String city) {
        log.info("Using CollectAPI for city {}, district {}", city, district);
        return collectApiClient.fetchOnDutyPharmacies(city, district)
                .flatMap(root -> {
                    if (root == null || !root.path("success").asBoolean()) {
                        return Mono.just(List.<Pharmacy>of());
                    }
                    JsonNode results = root.path("result");
                    return Flux.fromIterable(results)
                            .flatMap(node -> {
                                Pharmacy p = new Pharmacy();
                                p.setDisplayName(node.path("name").asText());
                                String loc = node.path("loc").asText();
                                if (loc != null && loc.contains(",")) {
                                    String[] parts = loc.split(",");
                                    p.setLat(Double.parseDouble(parts[0]));
                                    p.setLon(Double.parseDouble(parts[1]));
                                    return Mono.just(p);
                                } else {
                                    String address = node.path("address").asText();
                                    if (!PharmacyFinderUtil.isNullOrBlank(address)) {
                                        return locationService.enrichPharmacyWithCoordinates(p, address, city);
                                    }
                                    return Mono.empty();
                                }
                            })
                            .collectList();
                })
                .onErrorReturn(List.of());
    }

    private Mono<List<Pharmacy>> fetchFromGovApi(String district, String city) {
        log.info("Using Turkish Gov API for city {}, district {}", city, district);
        try {
            String encodedDistrict = URLEncoder.encode(district, StandardCharsets.UTF_8);
            String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);

            return turkishGovClient.fetchOnDutyPharmacies(encodedDistrict, encodedCity)
                    .flatMap(response -> {
                        if (response == null || response.get("data") == null) {
                            return Mono.just(List.<Pharmacy>of());
                        }
                        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
                        return Flux.fromIterable(data)
                                .flatMap(item -> {
                                    Pharmacy pharmacy = new Pharmacy();
                                    pharmacy.setDisplayName((String) item.get("EczaneAdi"));
                                    String address = (String) item.get("Adres");
                                    if (!PharmacyFinderUtil.isNullOrBlank(address)) {
                                        return locationService.enrichPharmacyWithCoordinates(pharmacy, address, city);
                                    }
                                    return Mono.just(pharmacy);
                                })
                                .collectList();
                    })
                    .onErrorReturn(List.of());
        } catch (Exception e) {
            log.error("Gov API error", e);
            return Mono.just(List.of());
        }
    }
}
