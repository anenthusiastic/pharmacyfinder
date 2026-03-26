package com.fatih.pharmacyfinder.service;

import com.fatih.pharmacyfinder.model.Pharmacy;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class OnDutyPharmacyScraperService {

    private final LocationService locationService;

    public OnDutyPharmacyScraperService(LocationService locationService) {
        this.locationService = locationService;
    }

    public Mono<List<Pharmacy>> scrapeOnDutyPharmacies(String district, String city) {
        return Mono.fromCallable(() -> {
            List<Pharmacy> pharmacies = new ArrayList<>();
            String url = String.format("https://www.eczaneler.gen.tr/nobetci-eczane/%s/%s",
                    normalizeForUrl(city), normalizeForUrl(district));

            log.info("Scraping on-duty pharmacies from: {}", url);

            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();

            Elements pharmacyElements = doc
                    .select(".pharmacy-item, .eczane-item, div[class*='pharmacy'], div[class*='eczane']");

            for (Element element : pharmacyElements) {
                Pharmacy pharmacy = new Pharmacy();
                String name = element.select(".name, .pharmacy-name, h3, h4").text();
                // Pick address from the specific address selector or first <p> after title if missing
                String address = element.select(".address, .pharmacy-address, .eczane-adres").text();
                if (address.isEmpty()) {
                    address = element.select("p").first().text();
                }

                if (!name.isEmpty()) {
                    pharmacy.setDisplayName(name);
                    pharmacy.setAddress(address);
                }
                pharmacies.add(pharmacy);
            }
            return pharmacies;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(pharmacies -> {
            return Flux.fromIterable(pharmacies)
                    .flatMap(pharmacy -> {
                        if (pharmacy.getLat() == 0.0 && pharmacy.getAddress() != null && !pharmacy.getAddress().isBlank()) {
                            return locationService.enrichPharmacyWithCoordinates(pharmacy, pharmacy.getAddress(), city);
                        }
                        return Mono.just(pharmacy);
                    })
                    .collectList();
        })
        .doOnNext(list -> log.info("Scraped {} on-duty pharmacies for {}/{}", list.size(), city, district))
        .onErrorResume(e -> {
            log.warn("Error scraping on-duty pharmacies for {}/{}: {}. Falling back to empty list.", city, district, e.getMessage());
            return Mono.just(List.of());
        });
    }

    private String normalizeForUrl(String text) {
        if (text == null)
            return "";
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
