package com.workmatch.company;

import com.workmatch.applicant.Applicant;
import com.workmatch.applicant.ApplicantRepository;
import com.workmatch.applicant.Application;
import com.workmatch.applicant.JobApplicationRepository;
import com.workmatch.Label;
import com.workmatch.LabelRepository;
import com.workmatch.applicant.ApplicantLabel;
import com.workmatch.applicant.ApplicantLabelRepository;
import com.workmatch.job.Job;
import com.workmatch.job.JobLabel;
import com.workmatch.job.JobLabelRepository;
import com.workmatch.matching.MatchingService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@Controller
public class CompanyController {

    private final CompanyRepository companyRepository;
    private final com.workmatch.job.JobRepository jobRepository;
    private final JobApplicationRepository applicationRepository;
    private final ApplicantRepository applicantRepository;
    private final LabelRepository labelRepository;
    private final ApplicantLabelRepository applicantLabelRepository;
    private final JobLabelRepository jobLabelRepository;
    private final MatchingService matchingService;

    public CompanyController(CompanyRepository companyRepository,
                            com.workmatch.job.JobRepository jobRepository,
                            JobApplicationRepository applicationRepository,
                            ApplicantRepository applicantRepository,
                            LabelRepository labelRepository,
                            ApplicantLabelRepository applicantLabelRepository,
                            JobLabelRepository jobLabelRepository,
                            MatchingService matchingService) {
        this.companyRepository = companyRepository;
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.applicantRepository = applicantRepository;
        this.labelRepository = labelRepository;
        this.applicantLabelRepository = applicantLabelRepository;
        this.jobLabelRepository = jobLabelRepository;
        this.matchingService = matchingService;
    }

    @GetMapping("/company/api/{id}")
    @ResponseBody
    public Company getCompanyById(@PathVariable Long id) {
        return companyRepository.findById(id).orElse(null);
    }

    // Lists the applications sent to the logged-in company's jobs, with a match score
    @GetMapping("/api/company/applications")
    @ResponseBody
    public ResponseEntity<?> getCompanyApplications(HttpSession session) {
        String companyName = (String) session.getAttribute("companyName");
        if (companyName == null) {
            return ResponseEntity.status(401).body("Please log in first.");
        }

        List<Job> allJobs = jobRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Application a : applicationRepository.findByCompany(companyName)) {
            // Old rows may have no applicant id, so check first
            Applicant applicant = a.getApplicantId() != null
                    ? applicantRepository.findById(a.getApplicantId()).orElse(null)
                    : null;

            // Find the job this application is for
            Job job = null;
            for (Job j : allJobs) {
                if (a.getJobTitle() != null && a.getJobTitle().equals(j.getTitle())
                        && companyName.equals(j.getCompany())) {
                    job = j;
                    break;
                }
            }

            // Score stays empty when the applicant or the job cannot be found
            Integer score = null;
            String level = null;
            if (applicant != null && job != null) {
                MatchingService.MatchResult match = matchingService.calculateMatch(
                        applicant, job, loadJobLabels(job.getId()), loadApplicantLabels(applicant.getId()));
                score = match.getScore();
                level = match.getLevel();
            }

            Map<String, Object> row = new HashMap<>();
            row.put("id", a.getId());
            row.put("applicantName", applicant != null ? applicant.getFullName() : "Unknown");
            row.put("applicantEmail", applicant != null ? applicant.getEmail() : "");
            row.put("jobTitle", a.getJobTitle());
            row.put("status", a.getStatus());
            row.put("appliedOn", a.getAppliedOn());
            row.put("score", score);
            row.put("level", level);
            result.add(row);
        }
        return ResponseEntity.ok(result);
    }

    // Gets the full labels the company ticked for one job
    private List<Label> loadJobLabels(Long jobId) {
        List<Long> ids = new ArrayList<>();
        for (JobLabel jl : jobLabelRepository.findByJobId(jobId)) {
            ids.add(jl.getLabelId());
        }
        return labelRepository.findAllById(ids);
    }

    // Gets the full labels one applicant ticked
    private List<Label> loadApplicantLabels(Long applicantId) {
        List<Long> ids = new ArrayList<>();
        for (ApplicantLabel al : applicantLabelRepository.findByApplicantId(applicantId)) {
            ids.add(al.getLabelId());
        }
        return labelRepository.findAllById(ids);
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