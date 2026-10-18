package com.workmatch.job;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String company;
    private String location;
    private Double salaryMin;
    private Double salaryMax;
    private Integer minEducationRank;
    private Integer experienceRank;
    private String region;
    private String province;
    private String city;
    private String otherNotes;
    private LocalDate closingDate;

    public Job() {
    }

    public Job(String title, String company, String location) {
        this.title = title;
        this.company = company;
        this.location = location;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
    public Double getSalaryMin() { return salaryMin; }
    public void setSalaryMin(Double salaryMin) { this.salaryMin = salaryMin; }

    public Double getSalaryMax() { return salaryMax; }
    public void setSalaryMax(Double salaryMax) { this.salaryMax = salaryMax; }

    public Integer getMinEducationRank() { return minEducationRank; }
    public void setMinEducationRank(Integer minEducationRank) { this.minEducationRank = minEducationRank; }

    public Integer getExperienceRank() { return experienceRank; }
    public void setExperienceRank(Integer experienceRank) { this.experienceRank = experienceRank; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getOtherNotes() { return otherNotes; }
    public void setOtherNotes(String otherNotes) { this.otherNotes = otherNotes; }

    public LocalDate getClosingDate() { return closingDate; }
    public void setClosingDate(LocalDate closingDate) { this.closingDate = closingDate; }
}