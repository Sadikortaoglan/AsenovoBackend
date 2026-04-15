package com.saraasansor.api.location.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "location")
public class LocationDataLoaderProperties {

    private boolean loadOnStartup = true;
    private String dataResource = "data/cityData.json";
    private int minCityCount = 50;
    private int minDistrictCount = 100;
    private int minNeighborhoodCount = 1000;
    private int batchSize = 500;

    public boolean isLoadOnStartup() {
        return loadOnStartup;
    }

    public void setLoadOnStartup(boolean loadOnStartup) {
        this.loadOnStartup = loadOnStartup;
    }

    public String getDataResource() {
        return dataResource;
    }

    public void setDataResource(String dataResource) {
        this.dataResource = dataResource;
    }

    public int getMinCityCount() {
        return minCityCount;
    }

    public void setMinCityCount(int minCityCount) {
        this.minCityCount = minCityCount;
    }

    public int getMinDistrictCount() {
        return minDistrictCount;
    }

    public void setMinDistrictCount(int minDistrictCount) {
        this.minDistrictCount = minDistrictCount;
    }

    public int getMinNeighborhoodCount() {
        return minNeighborhoodCount;
    }

    public void setMinNeighborhoodCount(int minNeighborhoodCount) {
        this.minNeighborhoodCount = minNeighborhoodCount;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}
