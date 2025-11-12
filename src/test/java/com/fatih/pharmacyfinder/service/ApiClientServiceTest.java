package com.fatih.pharmacyfinder.service;

import com.fatih.pharmacyfinder.config.PharmacyProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApiClientServiceTest {

    private MockWebServer mockWebServer;
    private ApiClientService apiClientService;
    private PharmacyProperties properties;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        properties = new PharmacyProperties();
        properties.getApi().getNominatim().setBaseUrl(mockWebServer.url("/").toString().replaceAll("/$", ""));
        properties.getApi().getNominatim().setTimeout(Duration.ofSeconds(5));
        properties.getApi().getNominatim().setRetryAttempts(1);
        
        WebClient webClient = WebClient.builder().build();
        apiClientService = new ApiClientService(webClient, properties);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testGeocodeAddress_Success() {
        String mockResponse = "[{\"lat\":\"41.0082\",\"lon\":\"28.9784\",\"display_name\":\"Istanbul, Turkey\"}]";
        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(apiClientService.geocodeAddress("Istanbul"))
                .assertNext(result -> {
                    assertFalse(result.isEmpty());
                    assertEquals("41.0082", ((Map<String, Object>) result.get(0)).get("lat"));
                })
                .verifyComplete();
    }

    @Test
    void testGeocodeAddress_Error() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        StepVerifier.create(apiClientService.geocodeAddress("Invalid"))
                .assertNext(result -> assertTrue(result.isEmpty()))
                .verifyComplete();
    }
}