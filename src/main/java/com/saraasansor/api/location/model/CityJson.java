package com.saraasansor.api.location.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CityJson {

    private String name;
    private List<DistrictJson> counties = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<DistrictJson> getCounties() {
        return counties;
    }

    public void setCounties(List<DistrictJson> counties) {
        this.counties = counties == null ? new ArrayList<>() : counties;
    }
}
