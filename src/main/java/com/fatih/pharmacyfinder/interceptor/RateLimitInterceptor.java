package com.fatih.pharmacyfinder.interceptor;

import com.fatih.pharmacyfinder.config.PharmacyProperties;
import com.fatih.pharmacyfinder.config.RateLimitConfig;
import com.fatih.pharmacyfinder.service.MetricsService;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    
    private final ConcurrentHashMap<String, Bucket> buckets;
    private final RateLimitConfig rateLimitConfig;
    private final PharmacyProperties properties;
    private final MetricsService metricsService;
    
    public RateLimitInterceptor(ConcurrentHashMap<String, Bucket> buckets, 
                               RateLimitConfig rateLimitConfig, 
                               PharmacyProperties properties,
                               MetricsService metricsService) {
        this.buckets = buckets;
        this.rateLimitConfig = rateLimitConfig;
        this.properties = properties;
        this.metricsService = metricsService;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String clientIp = getClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(clientIp, k -> rateLimitConfig.createBucket(properties));
        
        if (bucket.tryConsume(1)) {
            return true;
        } else {
            metricsService.incrementRateLimitExceeded();
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("X-Rate-Limit-Retry-After-Seconds", "60");
            return false;
        }
    }
    
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip != null ? ip.split(",")[0].trim() : "unknown";
    }
}