package com.fatih.pharmacyfinder.service;

import com.fatih.pharmacyfinder.client.NominatimClient;
import com.fatih.pharmacyfinder.config.PharmacyProperties;
import com.fatih.pharmacyfinder.model.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

class LocationServiceTest {

    @Mock
    private NominatimClient nominatimClient;
    @Mock
    private PharmacyProperties properties;

    private LocationService locationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        locationService = new LocationService(nominatimClient, properties);
        when(properties.getDefaultCountryCode()).thenReturn("tr");
    }

    @Test
    void testEnrichLocationWithAddress() {
        Location input = new Location();
        input.setLat(38.16);
        input.setLon(27.35);

        Map<String, Object> mockResponse = Map.of(
            "address", Map.of(
                "town", "Torbalı",
                "city", "İzmir",
                "country_code", "tr"
            )
        );

        when(nominatimClient.reverseGeocode(anyDouble(), anyDouble())).thenReturn(Mono.just(mockResponse));

        Mono<Location> result = locationService.enrichLocationWithAddress(input);

        StepVerifier.create(result)
            .assertNext(loc -> {
                assertEquals("Torbalı", loc.getDistrict());
                assertEquals("İzmir", loc.getCity());
                assertEquals("tr", loc.getCountryCode());
            })
            .verifyComplete();
    }
}
