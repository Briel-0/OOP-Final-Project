package com.workmatch.company;

import jakarta.persistence.*;

@Entity
@Table(name = "companies")
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String companyName;
    private String secDtiNumber;
    private String website;
    private String email;
    private String password;
    private Boolean isVerified = true; // Auto-verify for seamless testing

    // Getters & Setters
    public Long getId() { return id; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getSecDtiNumber() { return secDtiNumber; }
    public void setSecDtiNumber(String secDtiNumber) { this.secDtiNumber = secDtiNumber; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }
}