package com.fatih.pharmacyfinder.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LocationNotDetectedException.class)
    public ResponseEntity<Map<String, String>> handleLocationNotDetected(LocationNotDetectedException e) {
        log.warn("Location error: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(NoPharmacyNearbyException.class)
    public ResponseEntity<Map<String, String>> handleNoPharmacyNearby(NoPharmacyNearbyException e) {
        log.info("Search result: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneralException(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Sistemde beklenmedik bir hata oluştu. Lütfen daha sonra tekrar deneyin."));
    }
}
