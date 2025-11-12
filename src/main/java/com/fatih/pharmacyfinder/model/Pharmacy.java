package com.fatih.pharmacyfinder.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Pharmacy {
    private String display_name;
    private double lat;
    private double lon;
    private double distance; // kullanıcı konumuna göre hesaplanacak
}
