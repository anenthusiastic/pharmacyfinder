package com.fatih.pharmacyfinder.exception;


public class NoPharmacyNearbyException extends RuntimeException{

    public NoPharmacyNearbyException(String message) {
        super(message);
    }
}
