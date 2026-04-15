package com.saraasansor.api.service;

import com.saraasansor.api.dto.LocationOptionDto;
import com.saraasansor.api.model.City;
import com.saraasansor.api.model.District;
import com.saraasansor.api.model.Neighborhood;
import com.saraasansor.api.repository.CityRepository;
import com.saraasansor.api.repository.DistrictRepository;
import com.saraasansor.api.repository.NeighborhoodRepository;
import com.saraasansor.api.repository.RegionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocationServiceTest {

    private final CityRepository cityRepository = mock(CityRepository.class);
    private final DistrictRepository districtRepository = mock(DistrictRepository.class);
    private final NeighborhoodRepository neighborhoodRepository = mock(NeighborhoodRepository.class);
    private final RegionRepository regionRepository = mock(RegionRepository.class);
    private final LocationService locationService = new LocationService(
            cityRepository,
            districtRepository,
            neighborhoodRepository,
            regionRepository
    );

    @Test
    void getCitiesReturnsBestCanonicalCityWhenSeedAndJsonNamesOverlap() {
        City oldSeedCity = city(2L, "Adiyaman");
        City importedCity = city(82L, "ADIYAMAN");

        when(cityRepository.search(eq(null), any(Pageable.class))).thenReturn(List.of(oldSeedCity, importedCity));
        when(districtRepository.countByCityId(2L)).thenReturn(1L);
        when(districtRepository.countByCityId(82L)).thenReturn(9L);

        List<LocationOptionDto> result = locationService.getCities(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(82L);
        assertThat(result.get(0).getName()).isEqualTo("ADIYAMAN");
    }

    @Test
    void getCitiesFiltersQueryUsingCanonicalTurkishInsensitiveMatch() {
        City oldSeedCity = city(2L, "Adiyaman");
        City importedCity = city(82L, "ADIYAMAN");
        City anotherCity = city(1L, "ADANA");

        when(cityRepository.search(eq(null), any(Pageable.class))).thenReturn(List.of(anotherCity, oldSeedCity, importedCity));
        when(districtRepository.countByCityId(2L)).thenReturn(1L);
        when(districtRepository.countByCityId(82L)).thenReturn(9L);

        List<LocationOptionDto> result = locationService.getCities("adi");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(82L);
    }

    @Test
    void getNeighborhoodsUsesLargeEnoughPageSizeForHighCountDistricts() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        City city = city(96L, "IĞDIR");
        District district = district(638L, city, "MERKEZ");
        when(districtRepository.findById(638L)).thenReturn(Optional.of(district));
        when(districtRepository.findAllWithCity()).thenReturn(List.of(district));

        locationService.getNeighborhoods(638L, null);

        verify(neighborhoodRepository).searchByDistrictIds(eq(List.of(638L)), eq(null), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5000);
    }

    @Test
    void getDistrictsUsesCanonicalCityGroupWhenOldSeedCityIdIsSelected() {
        City oldSeedCity = city(38L, "Igdir");
        City importedCity = city(96L, "IĞDIR");
        District oldMerkez = district(216L, oldSeedCity, "Merkez");
        District aralik = district(636L, importedCity, "ARALIK");
        District merkez = district(638L, importedCity, "MERKEZ");
        District tuzluca = district(639L, importedCity, "TUZLUCA");

        when(cityRepository.findById(38L)).thenReturn(Optional.of(oldSeedCity));
        when(cityRepository.findAll()).thenReturn(List.of(oldSeedCity, importedCity));
        when(districtRepository.searchByCityIds(eq(List.of(38L, 96L)), eq(null), any(Pageable.class)))
                .thenReturn(List.of(oldMerkez, aralik, merkez, tuzluca));
        when(neighborhoodRepository.countByDistrictId(216L)).thenReturn(1L);
        when(neighborhoodRepository.countByDistrictId(638L)).thenReturn(80L);

        List<LocationOptionDto> result = locationService.getDistricts(38L, null);

        assertThat(result).extracting(LocationOptionDto::getName)
                .containsExactly("ARALIK", "MERKEZ", "TUZLUCA");
        assertThat(result).extracting(LocationOptionDto::getId)
                .contains(638L)
                .doesNotContain(216L);
    }

    @Test
    void getNeighborhoodsUsesCanonicalDistrictGroupWhenOldSeedDistrictIdIsSelected() {
        City oldSeedCity = city(38L, "Igdir");
        City importedCity = city(96L, "IĞDIR");
        District oldMerkez = district(216L, oldSeedCity, "Merkez");
        District importedMerkez = district(638L, importedCity, "MERKEZ");
        Neighborhood oldNeighborhood = neighborhood(1L, oldMerkez, "Merkez Mahallesi");
        Neighborhood importedNeighborhood = neighborhood(900L, importedMerkez, "BAĞLAR MAH");

        when(districtRepository.findById(216L)).thenReturn(Optional.of(oldMerkez));
        when(districtRepository.findAllWithCity()).thenReturn(List.of(oldMerkez, importedMerkez));
        when(neighborhoodRepository.searchByDistrictIds(eq(List.of(216L, 638L)), eq(null), any(Pageable.class)))
                .thenReturn(List.of(oldNeighborhood, importedNeighborhood));

        List<LocationOptionDto> result = locationService.getNeighborhoods(216L, null);

        assertThat(result).extracting(LocationOptionDto::getName)
                .containsExactly("BAĞLAR MAH", "Merkez Mahallesi");
    }

    private City city(Long id, String name) {
        City city = new City();
        city.setId(id);
        city.setName(name);
        return city;
    }

    private District district(Long id, City city, String name) {
        District district = new District();
        district.setId(id);
        district.setCity(city);
        district.setName(name);
        return district;
    }

    private Neighborhood neighborhood(Long id, District district, String name) {
        Neighborhood neighborhood = new Neighborhood();
        neighborhood.setId(id);
        neighborhood.setDistrict(district);
        neighborhood.setName(name);
        return neighborhood;
    }
}
