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
public class IzmirOpenDataClient {

    private final WebClient webClient;
    private final PharmacyProperties properties;

    public IzmirOpenDataClient(WebClient webClient, PharmacyProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    public Mono<JsonNode> fetchOnDutyPharmacies() {
        return webClient.get()
                .uri(properties.getApi().getIzmir().getBaseUrl() + "/api/ibb/nobetcieczaneler")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(properties.getApi().getIzmir().getTimeout())
                .retryWhen(Retry.backoff(properties.getApi().getIzmir().getRetryAttempts(), Duration.ofSeconds(1)))
                .doOnError(error -> log.error("Izmir API failed", error))
                .onErrorResume(e -> Mono.empty());
    }
}
