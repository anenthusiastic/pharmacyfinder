package com.fatih.pharmacyfinder.service;

import com.fatih.pharmacyfinder.model.Pharmacy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PharmacyServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private PharmacyService pharmacyService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testFindNearbyPharmacies_NoPharmacyFound() {
        when(restTemplate.getForObject(anyString(), eq(Pharmacy[].class))).thenReturn(null);
        List<Pharmacy> result = pharmacyService.findPharmaciesBasedOnTime(38.233053950098096, 27.287171149947284, "5000");
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindNearbyPharmacies_PharmacyFound() {
        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setLat(41.0);
        pharmacy.setLon(29.0);
        when(restTemplate.getForObject(anyString(), eq(Pharmacy[].class))).thenReturn(new Pharmacy[]{pharmacy});
        when(restTemplate.getForObject(startsWith("http://router.project-osrm.org/route/v1/driving"), eq(Map.class)))
                .thenReturn(mockOsrmResponse());
        List<Pharmacy> result = pharmacyService.findPharmaciesBasedOnTime(41.0, 29.0, "1000");
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertTrue(result.get(0).getDistance() > 0);
    }

    private Map mockOsrmResponse() {
        return java.util.Map.of(
                "routes", List.of(java.util.Map.of("distance", 1234.0))
        );
    }

    @Test
    void testFindNearbyPharmacies_ExceptionHandling() {
        when(restTemplate.getForObject(anyString(), eq(Pharmacy[].class))).thenThrow(new RestClientException("API error"));
        List<Pharmacy> result = pharmacyService.findPharmaciesBasedOnTime(41.0, 29.0, "1000");
        assertTrue(result.isEmpty());
    }
}
