package com.workmatch.company;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@Controller
public class CompanyController {

    private final CompanyRepository companyRepository;
    private final com.workmatch.job.JobRepository jobRepository;

    public CompanyController(CompanyRepository companyRepository,
                            com.workmatch.job.JobRepository jobRepository) {
        this.companyRepository = companyRepository;
        this.jobRepository = jobRepository;
    }

    @GetMapping("/company/api/{id}")
    @ResponseBody
    public Company getCompanyById(@PathVariable Long id) {
        return companyRepository.findById(id).orElse(null);
    }

        // Used when the page sends JSON (login.html does this)
    @PostMapping(value = "/api/auth/company/register", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<?> registerCompanyJson(@RequestBody CompanyDto dto) {
        if (saveCompany(dto)) {
            return ResponseEntity.ok().body("Registered successfully");
        }
        return ResponseEntity.badRequest().body("Email already registered");
    }

    // Used when a normal HTML form is submitted
    @PostMapping(value = "/api/auth/company/register",
                 consumes = {"application/x-www-form-urlencoded", "multipart/form-data"})
    public String registerCompanyForm(CompanyDto dto) {
        if (saveCompany(dto)) {
            return "redirect:/login.html?registered";
        }
        return "redirect:/login.html?error=exists";
    }

    private boolean saveCompany(CompanyDto dto) {
        if (dto.getEmail() == null || dto.getPassword() == null) {
            return false;
        }
        if (companyRepository.findByEmail(dto.getEmail()).isPresent()) {
            return false;
        }
        Company company = new Company();
        company.setCompanyName(dto.getCompanyName());
        company.setSecDtiNumber(dto.getSecDtiNumber());
        company.setWebsite(dto.getWebsite());
        company.setEmail(dto.getEmail());
        company.setPassword(dto.getPassword());
        companyRepository.save(company);
        return true;
    }

    @PostMapping("/api/auth/company/login")
    @ResponseBody
    public ResponseEntity<?> loginCompany(@RequestBody CompanyDto dto, HttpSession session) {
        Optional<Company> company = companyRepository.findByEmail(dto.getEmail());
        if (company.isPresent() && company.get().getPassword().equals(dto.getPassword())) {
            session.setAttribute("companyEmail", company.get().getEmail());
            session.setAttribute("companyName", company.get().getCompanyName());
            return ResponseEntity.ok().body("/company-dashboard.html?companyId=" + company.get().getId());
        }
        return ResponseEntity.status(401).body("Invalid credentials");
    }

    @PostMapping("/company/api/jobs")
    @ResponseBody
    public com.workmatch.job.Job postJob(@RequestBody com.workmatch.job.Job job) {
        return jobRepository.save(job);
    }

    public static class CompanyDto {
        private String companyName;
        private String secDtiNumber;
        private String website;
        private String email;
        private String password;

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
    }
}