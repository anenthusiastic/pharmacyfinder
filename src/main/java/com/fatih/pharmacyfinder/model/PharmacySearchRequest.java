package com.fatih.pharmacyfinder.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PharmacySearchRequest {
    private Double lat;
    private Double lon;
    private String city;
    private String district;
    private String address;
    private String time;
}
