package com.fatih.pharmacyfinder.util;

import com.fatih.pharmacyfinder.config.PharmacyProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

@Slf4j
@Component
public class PharmacyFinderUtil {

    private final PharmacyProperties properties;

    public PharmacyFinderUtil(PharmacyProperties properties) {
        this.properties = properties;
    }

    public String generateViewbox(double userLat, double userLon, double radiusKm) {
        double deltaLat = radiusKm / properties.getSearch().getOneDegreeKm();
        double deltaLon = radiusKm / (properties.getSearch().getOneDegreeKm() * Math.cos(Math.toRadians(userLat)));
        double minLat = userLat - deltaLat;
        double maxLat = userLat + deltaLat;
        double minLon = userLon - deltaLon;
        double maxLon = userLon + deltaLon;
        return String.format("%s,%s,%s,%s", minLon, minLat, maxLon, maxLat);
    }

    public static boolean isNullOrBlank(String str) {
        return str == null || str.isBlank();
    }

    public static boolean areAllNullOrBlank(String... strings) {
        return Stream.of(strings).allMatch(PharmacyFinderUtil::isNullOrBlank);
    }
}
