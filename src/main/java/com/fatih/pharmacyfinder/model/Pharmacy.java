package com.fatih.pharmacyfinder.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("display_name")
    private String displayName;

    private String address;
    private double lat;
    private double lon;
    private double distance; // Calculated distance from user
}
