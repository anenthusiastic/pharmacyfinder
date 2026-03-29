package com.fatih.pharmacyfinder.client;

import com.fatih.pharmacyfinder.config.PharmacyProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;

class HolidayClientTest {

    private MockWebServer mockWebServer;
    private HolidayClient holidayClient;
    private PharmacyProperties properties;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        properties = new PharmacyProperties();
        properties.getApi().getNager().setBaseUrl(mockWebServer.url("/").toString());
        properties.getApi().getNager().setTimeout(Duration.ofSeconds(5));
        properties.getApi().getNager().setRetryAttempts(1);

        WebClient webClient = WebClient.builder().build();
        holidayClient = new HolidayClient(webClient, properties);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testIsTodayPublicHoliday_True() {
        String jsonResponse = "[{\"date\":\"2026-01-01\",\"name\":\"New Year\",\"localName\":\"Yılbaşı\"}]";
        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .addHeader("Content-Type", "application/json"));

        Mono<Boolean> result = holidayClient.isTodayPublicHoliday("tr");

        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();
    }
}
