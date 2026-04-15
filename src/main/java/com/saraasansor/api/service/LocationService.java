package com.saraasansor.api.service;

import com.saraasansor.api.dto.LocationOptionDto;
import com.saraasansor.api.location.util.LocationNameNormalizer;
import com.saraasansor.api.model.City;
import com.saraasansor.api.model.District;
import com.saraasansor.api.model.Neighborhood;
import com.saraasansor.api.repository.CityRepository;
import com.saraasansor.api.repository.DistrictRepository;
import com.saraasansor.api.repository.NeighborhoodRepository;
import com.saraasansor.api.repository.RegionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        List<City> cities = cityRepository.search(null, PageRequest.of(0, 300, Sort.by(Sort.Direction.ASC, "name")));
        String canonicalQuery = LocationNameNormalizer.canonicalKey(normalizeQuery(query));
        if (StringUtils.hasText(canonicalQuery)) {
            cities = cities.stream()
                    .filter(city -> LocationNameNormalizer.canonicalKey(city.getName()).contains(canonicalQuery))
                    .toList();
        }
        return selectBestCanonicalCities(cities).stream()
                .map(city -> new LocationOptionDto(city.getId(), city.getName()))
                .toList();
    }

    public List<LocationOptionDto> getDistricts(Long cityId, String query) {
        if (cityId == null) {
            throw new RuntimeException("cityId is required");
        }
        List<Long> canonicalCityIds = resolveCanonicalCityIds(cityId);
        List<District> districts = districtRepository.searchByCityIds(
                canonicalCityIds,
                normalizeQuery(query),
                PageRequest.of(0, 1000, Sort.by(Sort.Direction.ASC, "name"))
        );
        return selectBestCanonicalDistricts(districts).stream()
                .map(district -> new LocationOptionDto(district.getId(), district.getName()))
                .toList();
    }

    public List<LocationOptionDto> getNeighborhoods(Long districtId, String query) {
        if (districtId == null) {
            throw new RuntimeException("districtId is required");
        }
        List<Long> canonicalDistrictIds = resolveCanonicalDistrictIds(districtId);
        List<Neighborhood> neighborhoods = neighborhoodRepository.searchByDistrictIds(
                canonicalDistrictIds,
                normalizeQuery(query),
                PageRequest.of(0, 5000, Sort.by(Sort.Direction.ASC, "name"))
        );
        return selectCanonicalNeighborhoods(neighborhoods).stream()
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

    private List<City> selectBestCanonicalCities(List<City> cities) {
        Map<String, City> bestByCanonicalName = new LinkedHashMap<>();
        for (City city : cities) {
            String key = LocationNameNormalizer.canonicalKey(city.getName());
            City current = bestByCanonicalName.get(key);
            if (current == null || isBetterCityOption(city, current)) {
                bestByCanonicalName.put(key, city);
            }
        }
        return bestByCanonicalName.values().stream()
                .sorted(Comparator.comparing(City::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private boolean isBetterCityOption(City candidate, City current) {
        long candidateDistrictCount = districtRepository.countByCityId(candidate.getId());
        long currentDistrictCount = districtRepository.countByCityId(current.getId());
        if (candidateDistrictCount != currentDistrictCount) {
            return candidateDistrictCount > currentDistrictCount;
        }
        return candidate.getId() != null && current.getId() != null && candidate.getId() > current.getId();
    }

    private List<Long> resolveCanonicalCityIds(Long cityId) {
        City selectedCity = cityRepository.findById(cityId)
                .orElseThrow(() -> new RuntimeException("city not found"));
        String canonicalName = LocationNameNormalizer.canonicalKey(selectedCity.getName());
        return cityRepository.findAll().stream()
                .filter(city -> LocationNameNormalizer.canonicalKey(city.getName()).equals(canonicalName))
                .map(City::getId)
                .toList();
    }

    private List<Long> resolveCanonicalDistrictIds(Long districtId) {
        District selectedDistrict = districtRepository.findById(districtId)
                .orElseThrow(() -> new RuntimeException("district not found"));
        String canonicalCityName = LocationNameNormalizer.canonicalKey(selectedDistrict.getCity().getName());
        String canonicalDistrictName = LocationNameNormalizer.canonicalKey(selectedDistrict.getName());
        return districtRepository.findAllWithCity().stream()
                .filter(district -> LocationNameNormalizer.canonicalKey(district.getCity().getName()).equals(canonicalCityName))
                .filter(district -> LocationNameNormalizer.canonicalKey(district.getName()).equals(canonicalDistrictName))
                .map(District::getId)
                .toList();
    }

    private List<District> selectBestCanonicalDistricts(List<District> districts) {
        Map<String, District> bestByCanonicalName = new LinkedHashMap<>();
        for (District district : districts) {
            String key = LocationNameNormalizer.canonicalKey(district.getName());
            District current = bestByCanonicalName.get(key);
            if (current == null || isBetterDistrictOption(district, current)) {
                bestByCanonicalName.put(key, district);
            }
        }
        return bestByCanonicalName.values().stream()
                .sorted(Comparator.comparing(District::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private boolean isBetterDistrictOption(District candidate, District current) {
        long candidateNeighborhoodCount = neighborhoodRepository.countByDistrictId(candidate.getId());
        long currentNeighborhoodCount = neighborhoodRepository.countByDistrictId(current.getId());
        if (candidateNeighborhoodCount != currentNeighborhoodCount) {
            return candidateNeighborhoodCount > currentNeighborhoodCount;
        }
        return candidate.getId() != null && current.getId() != null && candidate.getId() > current.getId();
    }

    private List<Neighborhood> selectCanonicalNeighborhoods(List<Neighborhood> neighborhoods) {
        Map<String, Neighborhood> bestByCanonicalName = new LinkedHashMap<>();
        for (Neighborhood neighborhood : neighborhoods) {
            String key = LocationNameNormalizer.canonicalKey(neighborhood.getName());
            Neighborhood current = bestByCanonicalName.get(key);
            if (current == null || neighborhood.getId() > current.getId()) {
                bestByCanonicalName.put(key, neighborhood);
            }
        }
        return bestByCanonicalName.values().stream()
                .sorted(Comparator.comparing(Neighborhood::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
