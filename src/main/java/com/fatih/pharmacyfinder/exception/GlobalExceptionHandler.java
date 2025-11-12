package com.fatih.pharmacyfinder.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.RestClientException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = NoPharmacyNearbyException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public @ResponseBody ResponseEntity<Object> handleException(NoPharmacyNearbyException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ex.getMessage());
    }

    @ExceptionHandler(value = RestClientException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public @ResponseBody ResponseEntity<Object> handleException(RestClientException ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ex.getMessage());
    }
}
