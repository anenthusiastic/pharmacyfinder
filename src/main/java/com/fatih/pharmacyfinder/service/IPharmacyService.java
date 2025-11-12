package com.fatih.pharmacyfinder.service;

import com.fatih.pharmacyfinder.model.Pharmacy;

import java.util.List;

public interface IPharmacyService {

    List<Pharmacy> findPharmaciesBasedOnTime(Double lat, Double lon, String ipAddress);
    
}
