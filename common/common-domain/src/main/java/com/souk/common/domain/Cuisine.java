package com.souk.common.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "Cuisines", uniqueConstraints = {
        @UniqueConstraint(name = "uq_cuisine", columnNames = {"cuisinename", "category", "subcategory", "region"})
})
public class Cuisine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cuisine_id")
    private Long id;

    @Column(name = "cuisinename", nullable = false, length = 100)
    private String cuisineName;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "subcategory", length = 100)
    private String subcategory;

    @Column(name = "region", length = 100)
    private String region;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCuisineName() { return cuisineName; }
    public void setCuisineName(String cuisineName) { this.cuisineName = cuisineName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSubcategory() { return subcategory; }
    public void setSubcategory(String subcategory) { this.subcategory = subcategory; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
}

