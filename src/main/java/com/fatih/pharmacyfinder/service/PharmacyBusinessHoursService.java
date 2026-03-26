package com.fatih.pharmacyfinder.service;

import com.fatih.pharmacyfinder.client.HolidayClient;
import com.fatih.pharmacyfinder.exception.LocationNotDetectedException;
import com.fatih.pharmacyfinder.util.PharmacyFinderUtil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import reactor.core.publisher.Mono;

@Slf4j
@Service
public class PharmacyBusinessHoursService {

    private final HolidayClient holidayClient;

    public PharmacyBusinessHoursService(HolidayClient holidayClient) {
        this.holidayClient = holidayClient;
    }

    public Mono<Boolean> isPharmacyClosed(String countryCode, ZonedDateTime userTime) {
        if (userTime == null) {
            userTime = ZonedDateTime.now();
        }

        DayOfWeek today = userTime.getDayOfWeek();

        // Pazar günleri nöbetçi eczaneler çalışır
        if (today == DayOfWeek.SUNDAY) {
            log.info("Bugün Pazar - normal eczaneler kapalı");
            return Mono.just(true);
        }

        LocalTime openingTime = LocalTime.of(9, 0);
        LocalTime closingTime = LocalTime.of(19, 0);

        LocalTime time = userTime.toLocalTime();

        // Mesai saatleri dışında mıyız?
        boolean isOutsideBusinessHours = time.isBefore(openingTime) || time.isAfter(closingTime);
        if (isOutsideBusinessHours) {
            log.info("Mesai saatleri dışındayız ({}), normal eczaneler kapalı.", time);
            return Mono.just(true);
        }

        // Eğer buraya geldiyse: 09:00 - 19:00 arasındayız ve günlerden Pazar değil.
        // Ama bugün resmi tatil mi? Bunu bilmek için countryCode şart.
        if (PharmacyFinderUtil.isNullOrBlank(countryCode)) {
            log.warn("Tatil kontrolü yapılamadı çünkü ülke kodu (countryCode) eksik.");
            return Mono.error(
                    new LocationNotDetectedException("Tatil kontrolü yapılamadı. Konum bilgisi (ülke kodu) eksik."));
        }

        // Ülke koduna göre resmi tatil kontrolü (Nager.Date API üzerinden)
        return holidayClient.isTodayPublicHoliday(countryCode)
                .doOnNext(isHoliday -> {
                    if (isHoliday) {
                        log.info("Bugün ({}) resmi tatil, normal eczaneler kapalı kabul ediliyor.", countryCode);
                    } else {
                        log.info("Mesai saatleri içindeyiz ({}) ve tatil değil, eczaneler açık.", time);
                    }
                });
    }
}
