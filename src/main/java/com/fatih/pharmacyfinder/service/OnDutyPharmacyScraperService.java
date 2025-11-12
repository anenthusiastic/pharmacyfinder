package com.fatih.pharmacyfinder.service;

import com.fatih.pharmacyfinder.model.Pharmacy;
import com.fatih.pharmacyfinder.util.PharmacyFinderUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@Service
public class OnDutyPharmacyScraperService {

    private final PharmacyFinderUtil util;

    public OnDutyPharmacyScraperService(PharmacyFinderUtil util) {
        this.util = util;
    }

    public List<Pharmacy> scrapeOnDutyPharmacies(String district, String city) {
        List<Pharmacy> onDutyPharmacies = new ArrayList<>();
        
        try {
            String url = String.format("https://www.eczaneler.gen.tr/nobetci-eczane/%s/%s",
                normalizeForUrl(city), normalizeForUrl(district));
            
            log.info("Scraping on-duty pharmacies from: {}", url);
            
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();
            
            Elements pharmacyElements = doc.select(".pharmacy-item, .eczane-item, div[class*='pharmacy'], div[class*='eczane']");
            
            for (Element element : pharmacyElements) {
                Pharmacy pharmacy = new Pharmacy();
                String name = element.select(".name, .pharmacy-name, h3, h4").text();
                String address = element.select(".address, .pharmacy-address").text();
                
                if (!name.isEmpty()) {
                    pharmacy.setDisplay_name(name);
                    if (!address.isEmpty()) {
                        util.geocodeAddress(pharmacy, address, city);
                    }
                    onDutyPharmacies.add(pharmacy);
                }
            }
            
            log.info("Scraped {} on-duty pharmacies for {}/{}", onDutyPharmacies.size(), city, district);
            
        } catch (Exception e) {
            log.error("Error scraping on-duty pharmacies for {}/{}: {}", city, district, e.getMessage());
        }
        
        return onDutyPharmacies;
    }



    private String normalizeForUrl(String text) {
        if (text == null) return "";
        return text.toLowerCase()
                .replace("ı", "i")
                .replace("ğ", "g")
                .replace("ü", "u")
                .replace("ş", "s")
                .replace("ö", "o")
                .replace("ç", "c")
                .replace(" ", "-");
    }
}
