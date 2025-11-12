package com.fatih.pharmacyfinder.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Slf4j
@Service
public class PharmacyScheduleService {

    //TO DO  this class should be modified for determining holidays and working days and hours.
    private String weekdayClosingTime;

    private String saturdayClosingTime;

    public LocalTime getClosingTime() {
        DayOfWeek today = java.time.LocalDate.now().getDayOfWeek();
        
        if (today == DayOfWeek.SUNDAY) {
            log.info("Sunday - pharmacies are closed");
            return LocalTime.of(0, 0);
        } else if (today == DayOfWeek.SATURDAY) {
            log.info("Saturday - closing time: {}", saturdayClosingTime);
            return LocalTime.parse(saturdayClosingTime);
        } else {
            log.info("Weekday - closing time: {}", weekdayClosingTime);
            return LocalTime.parse(weekdayClosingTime);
        }
    }

    public boolean isAfterClosingTime(LocalTime closingTime) {
        LocalTime now = LocalTime.now();
        boolean afterClosing = now.isAfter(closingTime);
        log.info("Current time: {}, Closing time: {}, After closing: {}", now, closingTime, afterClosing);
        return afterClosing;
    }
}
