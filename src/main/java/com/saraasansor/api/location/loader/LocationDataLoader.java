package com.saraasansor.api.location.loader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saraasansor.api.location.config.LocationDataLoaderProperties;
import com.saraasansor.api.location.model.CityJson;
import com.saraasansor.api.location.model.DataMigration;
import com.saraasansor.api.location.model.DistrictJson;
import com.saraasansor.api.location.model.NeighborhoodGroupJson;
import com.saraasansor.api.location.model.NeighborhoodJson;
import com.saraasansor.api.location.repository.DataMigrationRepository;
import com.saraasansor.api.location.util.LocationNameNormalizer;
import com.saraasansor.api.model.City;
import com.saraasansor.api.model.District;
import com.saraasansor.api.model.Neighborhood;
import com.saraasansor.api.repository.CityRepository;
import com.saraasansor.api.repository.DistrictRepository;
import com.saraasansor.api.repository.NeighborhoodRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class LocationDataLoader {

    static final String MIGRATION_NAME = "LOCATION_DATA_V1";

    private static final Logger log = LoggerFactory.getLogger(LocationDataLoader.class);
    private static final String MIGRATION_LOCK_NAME = "asenovo_location_data_v1";

    private final CityRepository cityRepository;
    private final DistrictRepository districtRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final DataMigrationRepository dataMigrationRepository;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;
    private final LocationDataLoaderProperties properties;

    public LocationDataLoader(
            CityRepository cityRepository,
            DistrictRepository districtRepository,
            NeighborhoodRepository neighborhoodRepository,
            DataMigrationRepository dataMigrationRepository,
            EntityManager entityManager,
            ObjectMapper objectMapper,
            LocationDataLoaderProperties properties
    ) {
        this.cityRepository = cityRepository;
        this.districtRepository = districtRepository;
        this.neighborhoodRepository = neighborhoodRepository;
        this.dataMigrationRepository = dataMigrationRepository;
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Transactional
    public void loadIfNeeded() {
        if (!properties.isLoadOnStartup()) {
            log.info("Location data loading disabled by configuration");
            return;
        }

        if (!tryAcquireMigrationLock()) {
            log.info("Location data migration is already running in another instance, skipping this startup");
            return;
        }

        if (dataMigrationRepository.existsByName(MIGRATION_NAME)) {
            log.info("Location data migration {} already exists, skipping", MIGRATION_NAME);
            return;
        }

        log.info("Location data loading started");
        List<CityJson> cityData = readLocationData();
        PreparedLocationData preparedData = prepareLocationData(cityData);
        validateDataset(preparedData);

        ImportSummary summary = mergeLocationData(preparedData);
        verifyImportedData(preparedData);

        dataMigrationRepository.save(new DataMigration(MIGRATION_NAME, Instant.now()));

        log.info("{} cities inserted", summary.citiesInserted());
        log.info("{} districts inserted", summary.districtsInserted());
        log.info("{} neighborhoods inserted", summary.neighborhoodsInserted());
        log.info(
                "Location data migration {} completed. Existing matched: cities={}, districts={}, neighborhoods={}",
                MIGRATION_NAME,
                summary.citiesMatched(),
                summary.districtsMatched(),
                summary.neighborhoodsMatched()
        );
    }

    private boolean tryAcquireMigrationLock() {
        Object result = entityManager
                .createNativeQuery("SELECT pg_try_advisory_xact_lock(hashtext(:lockName))")
                .setParameter("lockName", MIGRATION_LOCK_NAME)
                .getSingleResult();
        return Boolean.TRUE.equals(result);
    }

    private List<CityJson> readLocationData() {
        ClassPathResource resource = new ClassPathResource(properties.getDataResource());
        if (!resource.exists()) {
            throw new IllegalStateException("Location data resource not found: " + properties.getDataResource());
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<>() {
            });
        } catch (IOException ex) {
            throw new IllegalStateException("Location data JSON could not be parsed: " + properties.getDataResource(), ex);
        }
    }

    private PreparedLocationData prepareLocationData(List<CityJson> cityData) {
        List<City> cities = new ArrayList<>();
        List<DistrictDraft> districtDrafts = new ArrayList<>();
        List<NeighborhoodDraft> neighborhoodDrafts = new ArrayList<>();
        Set<String> cityKeys = new LinkedHashSet<>();
        Set<String> districtKeys = new LinkedHashSet<>();
        Set<String> neighborhoodKeys = new LinkedHashSet<>();

        if (cityData == null) {
            return new PreparedLocationData(cities, districtDrafts, neighborhoodDrafts, cityKeys, districtKeys, neighborhoodKeys);
        }

        for (CityJson cityJson : cityData) {
            String cityName = normalizeName(cityJson.getName());
            if (!StringUtils.hasText(cityName) || !cityKeys.add(normalizedKey(cityName))) {
                continue;
            }

            City city = new City();
            city.setName(cityName);
            cities.add(city);

            for (DistrictJson districtJson : cityJson.getCounties()) {
                String districtName = normalizeName(districtJson.getName());
                String districtKey = districtKey(cityName, districtName);
                if (!StringUtils.hasText(districtName) || !districtKeys.add(districtKey)) {
                    continue;
                }

                districtDrafts.add(new DistrictDraft(cityName, districtName));
                collectNeighborhoods(cityName, districtName, districtJson, neighborhoodKeys, neighborhoodDrafts);
            }
        }

        return new PreparedLocationData(cities, districtDrafts, neighborhoodDrafts, cityKeys, districtKeys, neighborhoodKeys);
    }

    private void validateDataset(PreparedLocationData preparedData) {
        if (preparedData.cities().size() < properties.getMinCityCount()) {
            throw new IllegalStateException("Location data aborted because city count is unexpectedly small: "
                    + preparedData.cities().size() + " < " + properties.getMinCityCount());
        }
        if (preparedData.districtDrafts().size() < properties.getMinDistrictCount()) {
            throw new IllegalStateException("Location data aborted because district count is unexpectedly small: "
                    + preparedData.districtDrafts().size() + " < " + properties.getMinDistrictCount());
        }
        if (preparedData.neighborhoodDrafts().size() < properties.getMinNeighborhoodCount()) {
            throw new IllegalStateException("Location data aborted because neighborhood count is unexpectedly small: "
                    + preparedData.neighborhoodDrafts().size() + " < " + properties.getMinNeighborhoodCount());
        }
    }

    private ImportSummary mergeLocationData(PreparedLocationData preparedData) {
        Map<String, City> cityByName = loadExistingCities();
        List<City> missingCities = new ArrayList<>();

        for (City city : preparedData.cities()) {
            String cityKey = normalizedKey(city.getName());
            if (!cityByName.containsKey(cityKey)) {
                missingCities.add(city);
            }
        }

        for (City city : saveInBatches(missingCities, cityRepository::saveAll)) {
            cityByName.put(normalizedKey(city.getName()), city);
        }

        Map<String, District> districtByCityAndName = loadExistingDistricts();
        List<District> missingDistricts = new ArrayList<>();

        for (DistrictDraft draft : preparedData.districtDrafts()) {
            String key = districtKey(draft.cityName(), draft.districtName());
            if (districtByCityAndName.containsKey(key)) {
                continue;
            }
            City city = cityByName.get(normalizedKey(draft.cityName()));
            if (city == null) {
                throw new IllegalStateException("City was not available for district: " + draft.districtName());
            }
            District district = new District();
            district.setCity(city);
            district.setName(draft.districtName());
            missingDistricts.add(district);
        }

        for (District district : saveInBatches(missingDistricts, districtRepository::saveAll)) {
            districtByCityAndName.put(districtKey(district.getCity().getName(), district.getName()), district);
        }

        Map<String, Neighborhood> neighborhoodByDistrictAndName = loadExistingNeighborhoods();
        List<Neighborhood> missingNeighborhoods = new ArrayList<>();

        for (NeighborhoodDraft draft : preparedData.neighborhoodDrafts()) {
            String key = neighborhoodKey(draft.cityName(), draft.districtName(), draft.neighborhoodName());
            if (neighborhoodByDistrictAndName.containsKey(key)) {
                continue;
            }
            District district = districtByCityAndName.get(districtKey(draft.cityName(), draft.districtName()));
            if (district == null) {
                throw new IllegalStateException("District was not available for neighborhood: " + draft.neighborhoodName());
            }
            Neighborhood neighborhood = new Neighborhood();
            neighborhood.setDistrict(district);
            neighborhood.setName(draft.neighborhoodName());
            missingNeighborhoods.add(neighborhood);
        }

        for (Neighborhood neighborhood : saveInBatches(missingNeighborhoods, neighborhoodRepository::saveAll)) {
            neighborhoodByDistrictAndName.put(
                    neighborhoodKey(
                            neighborhood.getDistrict().getCity().getName(),
                            neighborhood.getDistrict().getName(),
                            neighborhood.getName()
                    ),
                    neighborhood
            );
        }

        return new ImportSummary(
                missingCities.size(),
                missingDistricts.size(),
                missingNeighborhoods.size(),
                preparedData.cityKeys().size() - missingCities.size(),
                preparedData.districtKeys().size() - missingDistricts.size(),
                preparedData.neighborhoodKeys().size() - missingNeighborhoods.size()
        );
    }

    private void verifyImportedData(PreparedLocationData preparedData) {
        Map<String, City> cityByName = loadExistingCities();
        Map<String, District> districtByCityAndName = loadExistingDistricts();
        Map<String, Neighborhood> neighborhoodByDistrictAndName = loadExistingNeighborhoods();

        if (!cityByName.keySet().containsAll(preparedData.cityKeys())) {
            throw new IllegalStateException("Location migration verification failed: missing city rows");
        }
        if (!districtByCityAndName.keySet().containsAll(preparedData.districtKeys())) {
            throw new IllegalStateException("Location migration verification failed: missing district rows");
        }
        if (!neighborhoodByDistrictAndName.keySet().containsAll(preparedData.neighborhoodKeys())) {
            throw new IllegalStateException("Location migration verification failed: missing neighborhood rows");
        }
    }

    private Map<String, City> loadExistingCities() {
        Map<String, City> cityByName = new LinkedHashMap<>();
        for (City city : cityRepository.findAll()) {
            cityByName.put(normalizedKey(city.getName()), city);
        }
        return cityByName;
    }

    private Map<String, District> loadExistingDistricts() {
        Map<String, District> districtByCityAndName = new LinkedHashMap<>();
        for (District district : districtRepository.findAllWithCity()) {
            districtByCityAndName.put(districtKey(district.getCity().getName(), district.getName()), district);
        }
        return districtByCityAndName;
    }

    private Map<String, Neighborhood> loadExistingNeighborhoods() {
        Map<String, Neighborhood> neighborhoodByDistrictAndName = new LinkedHashMap<>();
        for (Neighborhood neighborhood : neighborhoodRepository.findAllWithDistrictAndCity()) {
            neighborhoodByDistrictAndName.put(
                    neighborhoodKey(
                            neighborhood.getDistrict().getCity().getName(),
                            neighborhood.getDistrict().getName(),
                            neighborhood.getName()
                    ),
                    neighborhood
            );
        }
        return neighborhoodByDistrictAndName;
    }

    private void collectNeighborhoods(
            String cityName,
            String districtName,
            DistrictJson districtJson,
            Set<String> neighborhoodKeys,
            List<NeighborhoodDraft> neighborhoodDrafts
    ) {
        for (NeighborhoodGroupJson groupJson : districtJson.getDistricts()) {
            for (NeighborhoodJson neighborhoodJson : groupJson.getNeighborhoods()) {
                String neighborhoodName = normalizeName(neighborhoodJson.getName());
                String neighborhoodKey = neighborhoodKey(cityName, districtName, neighborhoodName);
                if (!StringUtils.hasText(neighborhoodName) || !neighborhoodKeys.add(neighborhoodKey)) {
                    continue;
                }
                neighborhoodDrafts.add(new NeighborhoodDraft(cityName, districtName, neighborhoodName));
            }
        }
    }

    private <T> List<T> saveInBatches(List<T> entities, BatchSaver<T> saver) {
        int batchSize = Math.max(properties.getBatchSize(), 1);
        List<T> saved = new ArrayList<>(entities.size());
        for (int start = 0; start < entities.size(); start += batchSize) {
            int end = Math.min(start + batchSize, entities.size());
            Iterable<T> batchResult = saver.saveAll(entities.subList(start, end));
            for (T entity : batchResult) {
                saved.add(entity);
            }
        }
        return saved;
    }

    private String normalizeName(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String districtKey(String cityName, String districtName) {
        return normalizedKey(cityName) + "|" + normalizedKey(districtName);
    }

    private String neighborhoodKey(String cityName, String districtName, String neighborhoodName) {
        return districtKey(cityName, districtName) + "|" + normalizedKey(neighborhoodName);
    }

    private String normalizedKey(String value) {
        return LocationNameNormalizer.canonicalKey(value);
    }

    @FunctionalInterface
    private interface BatchSaver<T> {
        Iterable<T> saveAll(List<T> entities);
    }

    private record DistrictDraft(String cityName, String districtName) {
    }

    private record NeighborhoodDraft(String cityName, String districtName, String neighborhoodName) {
    }

    private record PreparedLocationData(
            List<City> cities,
            List<DistrictDraft> districtDrafts,
            List<NeighborhoodDraft> neighborhoodDrafts,
            Set<String> cityKeys,
            Set<String> districtKeys,
            Set<String> neighborhoodKeys
    ) {
    }

    private record ImportSummary(
            int citiesInserted,
            int districtsInserted,
            int neighborhoodsInserted,
            int citiesMatched,
            int districtsMatched,
            int neighborhoodsMatched
    ) {
    }
}
