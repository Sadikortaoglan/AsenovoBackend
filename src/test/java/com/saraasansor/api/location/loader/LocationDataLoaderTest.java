package com.saraasansor.api.location.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saraasansor.api.location.config.LocationDataLoaderProperties;
import com.saraasansor.api.location.model.DataMigration;
import com.saraasansor.api.location.repository.DataMigrationRepository;
import com.saraasansor.api.model.City;
import com.saraasansor.api.model.District;
import com.saraasansor.api.model.Neighborhood;
import com.saraasansor.api.repository.CityRepository;
import com.saraasansor.api.repository.DistrictRepository;
import com.saraasansor.api.repository.NeighborhoodRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocationDataLoaderTest {

    private final CityRepository cityRepository = mock(CityRepository.class);
    private final DistrictRepository districtRepository = mock(DistrictRepository.class);
    private final NeighborhoodRepository neighborhoodRepository = mock(NeighborhoodRepository.class);
    private final DataMigrationRepository dataMigrationRepository = mock(DataMigrationRepository.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final LocationDataLoaderProperties properties = new LocationDataLoaderProperties();
    private final LocationDataLoader loader = new LocationDataLoader(
            cityRepository,
            districtRepository,
            neighborhoodRepository,
            dataMigrationRepository,
            entityManager,
            new ObjectMapper(),
            properties
    );

    @Test
    void skipsWhenMigrationAlreadyExists() {
        mockMigrationLock(true);
        when(dataMigrationRepository.existsByName(LocationDataLoader.MIGRATION_NAME)).thenReturn(true);

        loader.loadIfNeeded();

        verify(cityRepository, never()).saveAll(anyList());
        verify(districtRepository, never()).saveAll(anyList());
        verify(neighborhoodRepository, never()).saveAll(anyList());
    }

    @Test
    void abortsBeforeInsertWhenCityCountIsUnexpectedlySmall() {
        properties.setDataResource("data/location-small.json");
        properties.setMinCityCount(50);
        mockMigrationLock(true);
        when(dataMigrationRepository.existsByName(LocationDataLoader.MIGRATION_NAME)).thenReturn(false);

        assertThrows(IllegalStateException.class, loader::loadIfNeeded);

        verify(cityRepository, never()).saveAll(anyList());
        verify(districtRepository, never()).saveAll(anyList());
        verify(neighborhoodRepository, never()).saveAll(anyList());
        verify(dataMigrationRepository, never()).save(any());
    }

    @Test
    void completesMissingHierarchyWhenCityAlreadyExists() {
        properties.setDataResource("data/location-small.json");
        properties.setMinCityCount(1);
        properties.setMinDistrictCount(1);
        properties.setMinNeighborhoodCount(1);
        mockMigrationLock(true);
        when(dataMigrationRepository.existsByName(LocationDataLoader.MIGRATION_NAME)).thenReturn(false);

        City existingCity = new City();
        existingCity.setId(1L);
        existingCity.setName("ADANA");
        when(cityRepository.findAll()).thenReturn(List.of(existingCity));

        AtomicReference<List<District>> savedDistricts = new AtomicReference<>(List.of());
        AtomicInteger districtFindCalls = new AtomicInteger();
        when(districtRepository.findAllWithCity()).thenAnswer(invocation ->
                districtFindCalls.getAndIncrement() == 0 ? List.of() : savedDistricts.get()
        );
        when(districtRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<District> districts = new ArrayList<>(invocation.getArgument(0));
            savedDistricts.set(districts);
            return districts;
        });

        AtomicReference<List<Neighborhood>> savedNeighborhoods = new AtomicReference<>(List.of());
        AtomicInteger neighborhoodFindCalls = new AtomicInteger();
        when(neighborhoodRepository.findAllWithDistrictAndCity()).thenAnswer(invocation ->
                neighborhoodFindCalls.getAndIncrement() == 0 ? List.of() : savedNeighborhoods.get()
        );
        when(neighborhoodRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Neighborhood> neighborhoods = new ArrayList<>(invocation.getArgument(0));
            savedNeighborhoods.set(neighborhoods);
            return neighborhoods;
        });

        loader.loadIfNeeded();

        verify(cityRepository, never()).saveAll(anyList());
        verify(districtRepository).saveAll(anyList());
        verify(neighborhoodRepository).saveAll(anyList());
        verify(dataMigrationRepository).save(any(DataMigration.class));
    }

    private void mockMigrationLock(boolean acquired) {
        Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(acquired);
    }
}
