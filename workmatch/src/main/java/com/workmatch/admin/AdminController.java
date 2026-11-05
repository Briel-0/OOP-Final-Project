package com.workmatch.admin;

import com.workmatch.Label;
import com.workmatch.LabelRepository;
import com.workmatch.applicant.Applicant;
import com.workmatch.applicant.ApplicantLabel;
import com.workmatch.applicant.ApplicantLabelRepository;
import com.workmatch.applicant.ApplicantRepository;
import com.workmatch.applicant.Application;
import com.workmatch.applicant.JobApplicationRepository;
import com.workmatch.company.Company;
import com.workmatch.company.CompanyRepository;
import com.workmatch.job.Job;
import com.workmatch.job.JobLabel;
import com.workmatch.job.JobLabelRepository;
import com.workmatch.job.JobRepository;
import com.workmatch.matching.MatchingService;
import com.workmatch.notification.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
public class AdminController {

    private final AdminRepository adminRepository;
    private final CompanyRepository companyRepository;
    private final ApplicantRepository applicantRepository;
    private final JobRepository jobRepository;
    private final JobApplicationRepository applicationRepository;
    private final JobLabelRepository jobLabelRepository;
    private final ApplicantLabelRepository applicantLabelRepository;
    private final LabelRepository labelRepository;
    private final MatchingService matchingService;
    private final NotificationService notificationService;

    public AdminController(AdminRepository adminRepository,
                           CompanyRepository companyRepository,
                           ApplicantRepository applicantRepository,
                           JobRepository jobRepository,
                           JobApplicationRepository applicationRepository,
                           JobLabelRepository jobLabelRepository,
                           ApplicantLabelRepository applicantLabelRepository,
                           LabelRepository labelRepository,
                           MatchingService matchingService,
                           NotificationService notificationService) {
        this.adminRepository = adminRepository;
        this.companyRepository = companyRepository;
        this.applicantRepository = applicantRepository;
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.jobLabelRepository = jobLabelRepository;
        this.applicantLabelRepository = applicantLabelRepository;
        this.labelRepository = labelRepository;
        this.matchingService = matchingService;
        this.notificationService = notificationService;
    }

    // Every admin endpoint uses these two to block non-admins
    private boolean notAdmin(HttpSession session) {
        return session.getAttribute("adminId") == null;
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(403).body("Admins only.");
    }

    private List<Label> loadJobLabels(Long jobId) {
        List<Long> ids = new ArrayList<>();
        for (JobLabel jl : jobLabelRepository.findByJobId(jobId)) {
            ids.add(jl.getLabelId());
        }
        return labelRepository.findAllById(ids);
    }

    private List<Label> loadApplicantLabels(Long applicantId) {
        List<Long> ids = new ArrayList<>();
        for (ApplicantLabel al : applicantLabelRepository.findByApplicantId(applicantId)) {
            ids.add(al.getLabelId());
        }
        return labelRepository.findAllById(ids);
    }

    // Remote = has Remote and does not have On-site or Hybrid
    private boolean isRemote(List<Label> labels) {
        boolean remote = false;
        for (Label l : labels) {
            String n = l.getName();
            if ("On-site".equalsIgnoreCase(n) || "Hybrid".equalsIgnoreCase(n)) return false;
            if ("Remote".equalsIgnoreCase(n)) remote = true;
        }
        return remote;
    }

    @PostMapping("/api/auth/admin/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload, HttpSession session) {
        Admin admin = adminRepository.findFirstByEmail(payload.get("email"));
        if (admin != null && admin.getPassword() != null && admin.getPassword().equals(payload.get("password"))) {
            // Forget any applicant or company that was logged in before
            session.removeAttribute("applicantId");
            session.removeAttribute("applicantEmail");
            session.removeAttribute("companyEmail");
            session.removeAttribute("companyName");
            session.setAttribute("adminId", admin.getId());
            admin.setLastLogin(LocalDateTime.now());
            adminRepository.save(admin);
            return ResponseEntity.ok().body("/admin-dashboard.html");
        }
        return ResponseEntity.status(401).body("Invalid credentials");
    }

    // Tells the page which admin is logged in
    @GetMapping("/api/admin/me")
    public ResponseEntity<?> me(HttpSession session) {
        if (notAdmin(session)) return forbidden();
        Admin admin = adminRepository.findById((Long) session.getAttribute("adminId")).orElse(null);
        if (admin == null) return forbidden();
        Map<String, Object> res = new HashMap<>();
        res.put("fullName", admin.getFullName());
        res.put("email", admin.getEmail());
        return ResponseEntity.ok(res);
    }

    // ---------- 1. VERIFY COMPANIES ----------

    // status = PENDING, VERIFIED, REJECTED or ALL
    @GetMapping("/api/admin/companies")
    public ResponseEntity<?> companies(@RequestParam(defaultValue = "PENDING") String status, HttpSession session) {
        if (notAdmin(session)) return forbidden();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Company c : companyRepository.findAll()) {
            String s = c.getVerificationStatus() != null ? c.getVerificationStatus() : "VERIFIED";
            if (!"ALL".equals(status) && !status.equals(s)) continue;
            Map<String, Object> row = new HashMap<>();
            row.put("id", c.getId());
            row.put("companyName", c.getCompanyName());
            row.put("secDtiNumber", c.getSecDtiNumber());
            row.put("website", c.getWebsite());
            row.put("email", c.getEmail());
            row.put("status", s);
            row.put("rejectionReason", c.getRejectionReason());
            result.add(row);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/admin/companies/{id}/verify")
    public ResponseEntity<?> verifyCompany(@PathVariable Long id, HttpSession session) {
        if (notAdmin(session)) return forbidden();
        Company c = companyRepository.findById(id).orElse(null);
        if (c == null) return ResponseEntity.status(404).body("Company not found.");
        c.setVerificationStatus("VERIFIED");
        c.setRejectionReason(null);
        c.setIsVerified(true);
        companyRepository.save(c);
        notificationService.notifyCompany(c.getCompanyName(), "Company approved",
                "Your company was approved. You can now post jobs.", "dashboard");
        return ResponseEntity.ok().body("Company approved");
    }

    @PostMapping("/api/admin/companies/{id}/reject")
    public ResponseEntity<?> rejectCompany(@PathVariable Long id,
                                           @RequestBody Map<String, String> payload,
                                           HttpSession session) {
        if (notAdmin(session)) return forbidden();
        String reason = payload.get("reason");
        if (reason == null || reason.isBlank()) {
            return ResponseEntity.badRequest().body("Please give a reason.");
        }
        Company c = companyRepository.findById(id).orElse(null);
        if (c == null) return ResponseEntity.status(404).body("Company not found.");
        c.setVerificationStatus("REJECTED");
        c.setRejectionReason(reason.trim());
        c.setIsVerified(false);
        companyRepository.save(c);
        notificationService.notifyCompany(c.getCompanyName(), "Company not approved",
                "Reason: " + reason.trim(), "dashboard");
        return ResponseEntity.ok().body("Company rejected");
    }

    // ---------- 2. MANAGE JOB POSTS ----------

    @GetMapping("/api/admin/jobs")
    public ResponseEntity<?> jobs(HttpSession session) {
        if (notAdmin(session)) return forbidden();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Job job : jobRepository.findAll()) {
            List<String> accessibility = new ArrayList<>();
            List<String> workSetup = new ArrayList<>();
            for (Label l : loadJobLabels(job.getId())) {
                if ("ACCESSIBILITY".equals(l.getLabelGroup())) accessibility.add(l.getName());
                if ("WORK_SETUP".equals(l.getLabelGroup())) workSetup.add(l.getName());
            }
            Map<String, Object> row = new HashMap<>();
            row.put("id", job.getId());
            row.put("title", job.getTitle());
            row.put("company", job.getCompany());
            row.put("location", job.getLocation());
            row.put("closingDate", job.getClosingDate() != null ? job.getClosingDate().toString() : null);
            row.put("hidden", Boolean.TRUE.equals(job.getIsHidden()));
            row.put("hiddenReason", job.getHiddenReason());
            row.put("accessibility", accessibility);
            row.put("workSetup", workSetup);
            result.add(row);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/admin/jobs/{id}/hide")
    public ResponseEntity<?> hideJob(@PathVariable Long id,
                                     @RequestBody Map<String, String> payload,
                                     HttpSession session) {
        if (notAdmin(session)) return forbidden();
        String reason = payload.get("reason");
        if (reason == null || reason.isBlank()) {
            return ResponseEntity.badRequest().body("Please give a reason.");
        }
        Job job = jobRepository.findById(id).orElse(null);
        if (job == null) return ResponseEntity.status(404).body("Job not found.");
        job.setIsHidden(true);
        job.setHiddenReason(reason.trim());
        jobRepository.save(job);
        notificationService.notifyCompany(job.getCompany(), "Job post hidden",
                "Your job post " + job.getTitle() + " was hidden. Reason: " + reason.trim(), "dashboard");
        return ResponseEntity.ok().body("Job hidden");
    }

    @PostMapping("/api/admin/jobs/{id}/unhide")
    public ResponseEntity<?> unhideJob(@PathVariable Long id, HttpSession session) {
        if (notAdmin(session)) return forbidden();
        Job job = jobRepository.findById(id).orElse(null);
        if (job == null) return ResponseEntity.status(404).body("Job not found.");
        job.setIsHidden(false);
        job.setHiddenReason(null);
        jobRepository.save(job);
        return ResponseEntity.ok().body("Job visible again");
    }

    // ---------- 3. MANAGE USERS (no passwords, no disability ticks) ----------

    @GetMapping("/api/admin/users")
    public ResponseEntity<?> users(HttpSession session) {
        if (notAdmin(session)) return forbidden();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Applicant a : applicantRepository.findAll()) {
            Map<String, Object> row = new HashMap<>();
            row.put("type", "APPLICANT");
            row.put("id", a.getId());
            row.put("name", a.getFullName());
            row.put("email", a.getEmail());
            row.put("joined", a.getCreatedAt() != null ? a.getCreatedAt().toString() : null);
            row.put("suspended", Boolean.TRUE.equals(a.getIsSuspended()));
            result.add(row);
        }
        for (Company c : companyRepository.findAll()) {
            Map<String, Object> row = new HashMap<>();
            row.put("type", "COMPANY");
            row.put("id", c.getId());
            row.put("name", c.getCompanyName());
            row.put("email", c.getEmail());
            row.put("joined", c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
            row.put("suspended", Boolean.TRUE.equals(c.getIsSuspended()));
            result.add(row);
        }
        return ResponseEntity.ok(result);
    }

    // type is applicant or company. Suspends, or reactivates if already suspended.
    @PostMapping("/api/admin/users/{type}/{id}/suspend")
    public ResponseEntity<?> suspend(@PathVariable String type, @PathVariable Long id, HttpSession session) {
        if (notAdmin(session)) return forbidden();
        if ("applicant".equalsIgnoreCase(type)) {
            Applicant a = applicantRepository.findById(id).orElse(null);
            if (a == null) return ResponseEntity.status(404).body("User not found.");
            boolean now = !Boolean.TRUE.equals(a.getIsSuspended());
            a.setIsSuspended(now);
            applicantRepository.save(a);
            return ResponseEntity.ok().body(now ? "Suspended" : "Reactivated");
        }
        if ("company".equalsIgnoreCase(type)) {
            Company c = companyRepository.findById(id).orElse(null);
            if (c == null) return ResponseEntity.status(404).body("User not found.");
            boolean now = !Boolean.TRUE.equals(c.getIsSuspended());
            c.setIsSuspended(now);
            companyRepository.save(c);
            return ResponseEntity.ok().body(now ? "Suspended" : "Reactivated");
        }
        return ResponseEntity.badRequest().body("Unknown account type.");
    }

    // ---------- 6. STATISTICS (computed each time, nothing stored) ----------

    @GetMapping("/api/admin/stats")
    public ResponseEntity<?> stats(HttpSession session) {
        if (notAdmin(session)) return forbidden();

        int activeJobs = 0;
        int remoteJobs = 0;
        for (Job job : jobRepository.findAll()) {
            if (Boolean.TRUE.equals(job.getIsHidden())) continue;
            boolean expired = job.getClosingDate() != null && job.getClosingDate().isBefore(LocalDate.now());
            if (!expired) activeJobs++;
            if (isRemote(loadJobLabels(job.getId()))) remoteJobs++;
        }

        int hired = 0;
        int counted = 0;
        double sum = 0;
        List<Application> apps = applicationRepository.findAll();
        for (Application app : apps) {
            if ("Hired".equals(app.getStatus())) hired++;
            Applicant ap = app.getApplicantId() != null
                    ? applicantRepository.findById(app.getApplicantId()).orElse(null) : null;
            Job job = app.getJobId() != null
                    ? jobRepository.findById(app.getJobId()).orElse(null) : null;
            if (ap != null && job != null) {
                sum += matchingService.calculateMatch(ap, job,
                        loadJobLabels(job.getId()), loadApplicantLabels(ap.getId())).getScore();
                counted++;
            }
        }

        int pending = 0;
        for (Company c : companyRepository.findAll()) {
            if ("PENDING".equals(c.getVerificationStatus())) pending++;
        }

        Map<String, Object> res = new HashMap<>();
        res.put("totalApplicants", applicantRepository.count());
        res.put("totalCompanies", companyRepository.count());
        res.put("activeJobs", activeJobs);
        res.put("totalApplications", apps.size());
        res.put("hiredApplications", hired);
        res.put("averageMatch", counted > 0 ? Math.round(sum / counted) : 0);
        res.put("remoteJobs", remoteJobs);
        res.put("pendingCompanies", pending);
        return ResponseEntity.ok(res);
    }
}