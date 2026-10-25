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
import com.workmatch.notification.Notification;
import com.workmatch.notification.NotificationRepository;
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
    private final NotificationRepository notificationRepository;

    public CompanyController(CompanyRepository companyRepository,
                            com.workmatch.job.JobRepository jobRepository,
                            JobApplicationRepository applicationRepository,
                            ApplicantRepository applicantRepository,
                            LabelRepository labelRepository,
                            ApplicantLabelRepository applicantLabelRepository,
                            JobLabelRepository jobLabelRepository,
                            MatchingService matchingService,
                            NotificationRepository notificationRepository) {
        this.companyRepository = companyRepository;
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.applicantRepository = applicantRepository;
        this.labelRepository = labelRepository;
        this.applicantLabelRepository = applicantLabelRepository;
        this.jobLabelRepository = jobLabelRepository;
        this.matchingService = matchingService;
        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/company/api/{id}")
    @ResponseBody
    public Company getCompanyById(@PathVariable Long id) {
        return companyRepository.findById(id).orElse(null);
    }

    // Tells the page which company is logged in (from the session)
    @GetMapping("/api/company/me")
    @ResponseBody
    public ResponseEntity<?> getCompanyInfo(HttpSession session) {
        String companyName = (String) session.getAttribute("companyName");
        if (companyName == null) {
            return ResponseEntity.status(401).body("Please log in first.");
        }
        Map<String, Object> response = new HashMap<>();
        response.put("companyName", companyName);
        return ResponseEntity.ok(response);
    }

    // Only the logged-in company's notifications, newest first, plus the unread count
    @GetMapping("/api/company/notifications")
    @ResponseBody
    public ResponseEntity<?> getCompanyNotifications(HttpSession session) {
        String companyName = (String) session.getAttribute("companyName");
        if (companyName == null) {
            return ResponseEntity.status(401).body("Please log in first.");
        }
        return ResponseEntity.ok(notificationRepository.findByCompanyNameOrderBySentAtDesc(companyName));
    }

    // Marks only this company's notifications as read
    @PostMapping("/api/company/notifications/mark-read")
    @ResponseBody
    public ResponseEntity<?> markCompanyNotificationsRead(HttpSession session) {
        String companyName = (String) session.getAttribute("companyName");
        if (companyName == null) {
            return ResponseEntity.status(401).body("Please log in first.");
        }
        List<Notification> list = notificationRepository.findByCompanyNameOrderBySentAtDesc(companyName);
        for (Notification n : list) {
            n.setIsRead(true);
        }
        notificationRepository.saveAll(list);
        Map<String, String> res = new HashMap<>();
        res.put("status", "success");
        return ResponseEntity.ok(res);
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

    // Gives the full details of one applicant for the logged-in company
    @GetMapping("/api/company/applications/{id}")
    @ResponseBody
    public ResponseEntity<?> getApplicationDetails(@PathVariable Long id, HttpSession session) {
        String companyName = (String) session.getAttribute("companyName");
        if (companyName == null) {
            return ResponseEntity.status(401).body("Please log in first.");
        }

        // The application must exist and belong to this company
        Application a = applicationRepository.findById(id).orElse(null);
        if (a == null || !companyName.equals(a.getCompany()) || a.getApplicantId() == null) {
            return ResponseEntity.status(404).body("Application not found.");
        }
        Applicant applicant = applicantRepository.findById(a.getApplicantId()).orElse(null);
        if (applicant == null) {
            return ResponseEntity.status(404).body("Applicant not found.");
        }
    

        // Sort the applicant's ticked labels by group
        List<String> skills = new ArrayList<>();
        List<String> workSetup = new ArrayList<>();
        List<String> accessibilityNeeds = new ArrayList<>();
        List<String> physicalLimits = new ArrayList<>();
        for (Label l : loadApplicantLabels(applicant.getId())) {
            if ("SKILL".equals(l.getLabelGroup())) skills.add(l.getName());
            else if ("WORK_SETUP".equals(l.getLabelGroup())) workSetup.add(l.getName());
            else if ("ACCESSIBILITY".equals(l.getLabelGroup())) accessibilityNeeds.add(l.getName());
            else if ("PHYSICAL_DEMAND".equals(l.getLabelGroup())) physicalLimits.add(l.getName());
        }
    

        // Education name comes from the education label with the same rank
        String education = "";
        if (applicant.getEducationRank() != null) {
            for (Label l : labelRepository.findAll()) {
                if ("EDUCATION".equals(l.getLabelGroup())
                        && applicant.getEducationRank().equals(l.getRankOrder())) {
                    education = l.getName();
                }
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("applicantName", applicant.getFullName());
        response.put("applicantEmail", applicant.getEmail());
        response.put("jobTitle", a.getJobTitle());
        response.put("status", a.getStatus());
        response.put("appliedOn", a.getAppliedOn());
        response.put("education", education);
        response.put("program", applicant.getProgram());
        response.put("experienceYears", applicant.getExperienceYears());
        response.put("region", applicant.getRegion());
        response.put("province", applicant.getProvince());
        response.put("city", applicant.getCity());
        response.put("otherNeeds", applicant.getOtherNeeds());
        response.put("skills", skills);
        response.put("workSetup", workSetup);
        response.put("accessibilityNeeds", accessibilityNeeds);
        response.put("physicalLimits", physicalLimits);
        return ResponseEntity.ok(response);
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

        // Lets the logged-in company change the status of one of its applications
        @PostMapping("/api/company/applications/{id}/status")
        @ResponseBody
        public ResponseEntity<?> changeApplicationStatus(@PathVariable Long id,
                                                        @RequestBody Map<String, String> payload,
                                                        HttpSession session) {
            String companyName = (String) session.getAttribute("companyName");
            if (companyName == null) {
                return ResponseEntity.status(401).body("Please log in first.");
            }

            // Only these statuses can be picked by the company
            List<String> allowed = List.of("Under review", "Interview invited", "Offer sent", "Hired", "Not selected");
            String newStatus = payload.get("status");
            if (newStatus == null || !allowed.contains(newStatus)) {
                return ResponseEntity.badRequest().body("Invalid status.");
            }

            // The application must exist and belong to this company
            Application a = applicationRepository.findById(id).orElse(null);
            if (a == null || !companyName.equals(a.getCompany())) {
                return ResponseEntity.status(404).body("Application not found.");
            }

            a.setStatus(newStatus);
            applicationRepository.save(a);
            return ResponseEntity.ok().body("Status updated");
        }

        // Company accepts an applicant: they move to the Interviews page
        @PostMapping("/api/company/applications/{id}/accept")
        @ResponseBody
        public ResponseEntity<?> acceptApplication(@PathVariable Long id, HttpSession session) {
            String companyName = (String) session.getAttribute("companyName");
            if (companyName == null) {
                return ResponseEntity.status(401).body("Please log in first.");
            }

            // The application must exist and belong to this company
            Application a = applicationRepository.findById(id).orElse(null);
            if (a == null || !companyName.equals(a.getCompany())) {
                return ResponseEntity.status(404).body("Application not found.");
            }

            // Only new applications can be accepted
            if (!"Submitted".equals(a.getStatus()) && !"Under review".equals(a.getStatus())) {
                return ResponseEntity.badRequest().body("This application can no longer be accepted.");
            }

            a.setStatus("Interview invited");
            applicationRepository.save(a);
            return ResponseEntity.ok().body("Applicant accepted");
        }

        // Company rejects an applicant and saves the reason
        @PostMapping("/api/company/applications/{id}/reject")
        @ResponseBody
        public ResponseEntity<?> rejectApplication(@PathVariable Long id,
                                                @RequestBody Map<String, String> payload,
                                                HttpSession session) {
            String companyName = (String) session.getAttribute("companyName");
            if (companyName == null) {
                return ResponseEntity.status(401).body("Please log in first.");
            }

            String reason = payload.get("reason");
            if (reason == null || reason.isBlank()) {
                return ResponseEntity.badRequest().body("Please pick a reason.");
            }

            // The application must exist and belong to this company
            Application a = applicationRepository.findById(id).orElse(null);
            if (a == null || !companyName.equals(a.getCompany())) {
                return ResponseEntity.status(404).body("Application not found.");
            }

            // A hired or already rejected applicant cannot be rejected again
            if ("Hired".equals(a.getStatus()) || "Not selected".equals(a.getStatus())) {
                return ResponseEntity.badRequest().body("This application is already closed.");
            }

            a.setStatus("Not selected");
            a.setRejectionReason(reason.trim());
            applicationRepository.save(a);
            return ResponseEntity.ok().body("Applicant rejected");
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
            // Forget any applicant that was logged in before
            session.removeAttribute("applicantEmail");
            session.removeAttribute("applicantId");
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