package com.workmatch.applicant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Applicant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String email;
    private String disabilityType;
    private String password;

    // Added profile fields for Step 2
    private Integer educationRank;
    private Double experienceYears;
    private String region;
    private String province;
    private String city;
    private String otherNeeds;
    private String program;

    public Applicant() {}

    public Applicant(String fullName, String email, String disabilityType, String password) {
        this.fullName = fullName;
        this.email = email;
        this.disabilityType = disabilityType;
        this.password = password;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisabilityType() { return disabilityType; }
    public void setDisabilityType(String disabilityType) { this.disabilityType = disabilityType; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    // Added getters and setters for Step 2 profile fields
    public Integer getEducationRank() { return educationRank; }
    public void setEducationRank(Integer educationRank) { this.educationRank = educationRank; }

    public Double getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Double experienceYears) { this.experienceYears = experienceYears; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getOtherNeeds() { return otherNeeds; }
    public void setOtherNeeds(String otherNeeds) { this.otherNeeds = otherNeeds; }

    public String getProgram() { return program; }
    public void setProgram(String program) { this.program = program; }
}