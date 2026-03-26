package com.fatih.pharmacyfinder.client;

import com.fatih.pharmacyfinder.config.PharmacyProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class HolidayClient {

    private final WebClient webClient;
    private final PharmacyProperties properties;

    public HolidayClient(WebClient webClient, PharmacyProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    public Mono<Boolean> isTodayPublicHoliday(String countryCode) {
        return webClient.get()
                .uri(properties.getApi().getNager().getBaseUrl() + "/api/v3/IsTodayPublicHoliday/"
                        + countryCode.toUpperCase())
                .exchangeToMono(response -> {
                    int statusCode = response.statusCode().value();
                    if (statusCode == 200) {
                        return Mono.just(true);
                    } else if (statusCode == 204) {
                        return Mono.just(false);
                    } else {
                        log.error("Holiday API returned status code: {} for {}", statusCode, countryCode);
                        return response.createException().flatMap(Mono::error);
                    }
                })
                .timeout(properties.getApi().getNager().getTimeout())
                .doOnError(e -> log.error("Holiday check execution error for {}: {}", countryCode, e.getMessage()));
    }
}
