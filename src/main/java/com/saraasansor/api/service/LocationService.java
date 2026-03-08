package com.saraasansor.api.service;

import com.saraasansor.api.dto.LocationOptionDto;
import com.saraasansor.api.repository.CityRepository;
import com.saraasansor.api.repository.DistrictRepository;
import com.saraasansor.api.repository.NeighborhoodRepository;
import com.saraasansor.api.repository.RegionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class LocationService {

    private final CityRepository cityRepository;
    private final DistrictRepository districtRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final RegionRepository regionRepository;

    public LocationService(CityRepository cityRepository,
                           DistrictRepository districtRepository,
                           NeighborhoodRepository neighborhoodRepository,
                           RegionRepository regionRepository) {
        this.cityRepository = cityRepository;
        this.districtRepository = districtRepository;
        this.neighborhoodRepository = neighborhoodRepository;
        this.regionRepository = regionRepository;
    }

    public List<LocationOptionDto> getCities(String query) {
        return cityRepository.search(normalizeQuery(query), PageRequest.of(0, 200, Sort.by(Sort.Direction.ASC, "name")))
                .stream()
                .map(city -> new LocationOptionDto(city.getId(), city.getName()))
                .toList();
    }

    public List<LocationOptionDto> getDistricts(Long cityId, String query) {
        if (cityId == null) {
            throw new RuntimeException("cityId is required");
        }
        return districtRepository.search(cityId, normalizeQuery(query), PageRequest.of(0, 500, Sort.by(Sort.Direction.ASC, "name")))
                .stream()
                .map(district -> new LocationOptionDto(district.getId(), district.getName()))
                .toList();
    }

    public List<LocationOptionDto> getNeighborhoods(Long districtId, String query) {
        if (districtId == null) {
            throw new RuntimeException("districtId is required");
        }
        return neighborhoodRepository.search(districtId, normalizeQuery(query), PageRequest.of(0, 500, Sort.by(Sort.Direction.ASC, "name")))
                .stream()
                .map(neighborhood -> new LocationOptionDto(neighborhood.getId(), neighborhood.getName()))
                .toList();
    }

    public List<LocationOptionDto> getRegions(Long neighborhoodId, String query) {
        if (neighborhoodId == null) {
            throw new RuntimeException("neighborhoodId is required");
        }
        return regionRepository.search(neighborhoodId, normalizeQuery(query), PageRequest.of(0, 500, Sort.by(Sort.Direction.ASC, "name")))
                .stream()
                .map(region -> new LocationOptionDto(region.getId(), region.getName()))
                .toList();
    }

    private String normalizeQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return null;
        }
        return query.trim();
    }
}
