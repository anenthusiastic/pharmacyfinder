package com.fatih.pharmacyfinder.client;

import com.fatih.pharmacyfinder.config.PharmacyProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
public class TurkishGovClient {
    
    private final WebClient webClient;
    private final PharmacyProperties properties;
    
    public TurkishGovClient(WebClient webClient, PharmacyProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }
    
    public Mono<Map<String, Object>> fetchOnDutyPharmacies(String district, String city) {
        return webClient.get()
                .uri(properties.getApi().getTurkishGov().getBaseUrl() + 
                     "/saglik-bakanligi-eczane-nobetci-sorgulama?ilce={district}&il={city}", district, city)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(properties.getApi().getTurkishGov().getTimeout())
                .retryWhen(Retry.backoff(properties.getApi().getTurkishGov().getRetryAttempts(), Duration.ofSeconds(1)))
                .doOnError(error -> log.error("Turkish Gov API failed for district: {}, city: {}", district, city, error))
                .onErrorReturn(Map.of());
    }
}
