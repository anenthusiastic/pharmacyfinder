package com.fatih.pharmacyfinder.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fatih.pharmacyfinder.config.PharmacyProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
@Component
public class CollectApiClient {

    private final WebClient webClient;
    private final PharmacyProperties properties;

    public CollectApiClient(WebClient webClient, PharmacyProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    public Mono<JsonNode> fetchOnDutyPharmacies(String city, String district) {
        return webClient.get()
                .uri(properties.getApi().getCollect().getBaseUrl() + "/health/dutyPharmacy?ilce={district}&il={city}",
                        district, city)
                .header("authorization", "apikey " + properties.getApi().getCollect().getApiKey())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(properties.getApi().getCollect().getTimeout())
                .retryWhen(Retry.backoff(properties.getApi().getCollect().getRetryAttempts(), Duration.ofSeconds(1)))
                .doOnError(error -> log.error("CollectAPI failed for city: {}, district: {}", city, district, error))
                .onErrorResume(e -> Mono.empty());
    }
}
