package com.fatih.pharmacyfinder.service;

import com.fatih.pharmacyfinder.model.Pharmacy;
import com.fatih.pharmacyfinder.util.PharmacyFinderUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OnDutyPharmacyService {

    private final ApiClientService apiClientService;
    private final OnDutyPharmacyScraperService scraperService;
    private final PharmacyFinderUtil pharmacyFinderUtil;
    private final ValidationService validationService;

    public OnDutyPharmacyService(ApiClientService apiClientService, 
                                OnDutyPharmacyScraperService scraperService,
                                PharmacyFinderUtil pharmacyFinderUtil,
                                ValidationService validationService) {
        this.apiClientService = apiClientService;
        this.scraperService = scraperService;
        this.pharmacyFinderUtil = pharmacyFinderUtil;
        this.validationService = validationService;
    }

    @Cacheable(value = "pharmacies", key = "#district + '_' + #city")
    public List<Pharmacy> getOnDutyPharmacies(String district, String city) {
        if (!validationService.isValidCityOrDistrict(district) || !validationService.isValidCityOrDistrict(city)) {
            log.warn("Invalid district or city provided: {}, {}", district, city);
            return List.of();
        }
        
        String sanitizedDistrict = validationService.sanitizeInput(district);
        String sanitizedCity = validationService.sanitizeInput(city);
        
        List<Pharmacy> pharmacies = fetchFromApi(sanitizedDistrict, sanitizedCity);
        
        if (pharmacies.isEmpty()) {
            log.info("API returned no results, falling back to scraping");
            pharmacies = scraperService.scrapeOnDutyPharmacies(sanitizedDistrict, sanitizedCity);
        }
        
        return pharmacies;
    }

    private List<Pharmacy> fetchFromApi(String district, String city) {
        try {
            String encodedDistrict = URLEncoder.encode(district, StandardCharsets.UTF_8);
            String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
            
            Map<String, Object> response = apiClientService.fetchTurkishGovData(encodedDistrict, encodedCity).block();
            
            if (response != null && response.get("data") != null) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
                List<Pharmacy> pharmacies = new ArrayList<>();
                
                for (Map<String, Object> item : data) {
                    Pharmacy pharmacy = new Pharmacy();
                    pharmacy.setDisplay_name((String) item.get("EczaneAdi"));
                    
                    String address = (String) item.get("Adres");
                    if (address != null) {
                        pharmacyFinderUtil.geocodeAddress(pharmacy, address, city);
                    }
                    
                    pharmacies.add(pharmacy);
                }
                
                log.info("Fetched {} on-duty pharmacies from Turkish Gov API", pharmacies.size());
                return pharmacies;
            }
        } catch (Exception e) {
            log.error("Error fetching from Turkish Gov API: {}", e.getMessage());
        }
        
        return List.of();
    }

}
