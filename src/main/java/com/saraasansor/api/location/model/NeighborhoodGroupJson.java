package com.saraasansor.api.location.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NeighborhoodGroupJson {

    private String name;
    private List<NeighborhoodJson> neighborhoods = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<NeighborhoodJson> getNeighborhoods() {
        return neighborhoods;
    }

    public void setNeighborhoods(List<NeighborhoodJson> neighborhoods) {
        this.neighborhoods = neighborhoods == null ? new ArrayList<>() : neighborhoods;
    }
}
