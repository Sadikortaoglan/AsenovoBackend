package com.saraasansor.api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "neighborhoods", uniqueConstraints = {
        @UniqueConstraint(name = "uk_neighborhoods_district_name", columnNames = {"district_id", "name"})
})
public class Neighborhood {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id", nullable = false)
    private District district;

    @Column(nullable = false)
    private String name;

    public Neighborhood() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public District getDistrict() {
        return district;
    }

    public void setDistrict(District district) {
        this.district = district;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
