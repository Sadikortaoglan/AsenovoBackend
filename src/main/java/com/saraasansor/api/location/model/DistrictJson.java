package com.saraasansor.api.location.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DistrictJson {

    private String name;
    private List<NeighborhoodGroupJson> districts = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<NeighborhoodGroupJson> getDistricts() {
        return districts;
    }

    public void setDistricts(List<NeighborhoodGroupJson> districts) {
        this.districts = districts == null ? new ArrayList<>() : districts;
    }
}
