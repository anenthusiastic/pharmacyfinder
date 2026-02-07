package com.fatih.pharmacyfinder.client;

import com.fatih.pharmacyfinder.config.PharmacyProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
public class IpLocationClient {
    
    private final WebClient webClient;
    private final PharmacyProperties properties;
    
    public IpLocationClient(WebClient webClient, PharmacyProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }
    
    @Cacheable(value = "locations", key = "#ip")
    public Mono<Map<String, Object>> getLocationByIp(String ip) {
        return webClient.get()
                .uri(properties.getApi().getIpLocation().getBaseUrl() + "/json/{ip}", ip)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(properties.getApi().getIpLocation().getTimeout())
                .retryWhen(Retry.backoff(properties.getApi().getIpLocation().getRetryAttempts(), Duration.ofSeconds(1)))
                .doOnError(error -> log.error("IP location lookup failed for IP: {}", ip, error))
                .onErrorReturn(Map.of());
    }
}
