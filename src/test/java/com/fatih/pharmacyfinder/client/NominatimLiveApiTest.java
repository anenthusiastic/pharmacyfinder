package com.fatih.pharmacyfinder.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Tag("live")
class NominatimLiveApiTest {

    @Autowired
    private NominatimClient nominatimClient;

    @Test
    @DisplayName("GERÇEK API: Geocode araması başarılı olmalı")
    void testGeocodeAddress_RealRequest() {
        String query = "Torbalı, İzmir";

        nominatimClient.geocodeAddress(query)
                .as(StepVerifier::create)
                .assertNext(results -> {
                    assertNotNull(results);
                    assertFalse(results.isEmpty(), "Sonuç listesi boş olmamalı");

                    Map<String, Object> firstResult = results.get(0);
                    assertTrue(firstResult.get("display_name").toString().contains("Torbalı"),
                            "Dönen sonuç Torbalı içermeli");

                    System.out.println("Gerçek API Yanıtı (Geocode): " + firstResult.get("display_name"));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("GERÇEK API: Ters geocode (koordinattan adres) başarılı olmalı")
    void testReverseGeocode_RealRequest() {
        // İzmir Saat Kulesi koordinatları
        double lat = 38.4189;
        double lon = 27.1287;

        nominatimClient.reverseGeocode(lat, lon)
                .as(StepVerifier::create)
                .assertNext(result -> {
                    assertNotNull(result);
                    assertTrue(result.containsKey("address"), "Yanıt adres içermeli");

                    @SuppressWarnings("unchecked")
                    Map<String, Object> address = (Map<String, Object>) result.get("address");
                    // Konak veya İzmir içerdiğini doğrula
                    assertTrue(address.toString().toLowerCase().contains("izmir"),
                            "Dönen adres İzmir içermeli");

                    System.out.println("Gerçek API Yanıtı (Reverse): " + result.get("display_name"));
                })
                .verifyComplete();
    }
}
