package com.workmatch.applicant;

import com.workmatch.job.Job;
import com.workmatch.job.JobRepository;
import com.workmatch.message.Message;
import com.workmatch.message.MessageRepository;
import com.workmatch.notification.Notification;
import com.workmatch.notification.NotificationRepository;
import com.workmatch.Label;
import com.workmatch.LabelRepository;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
public class ApplicantController {

    private final JobRepository jobRepository;
    private final JobApplicationRepository applicationRepository;
    private final MessageRepository messageRepository;
    private final NotificationRepository notificationRepository;
    private final ApplicantRepository applicantRepository;
    private final ApplicantLabelRepository applicantLabelRepository;
    private final LabelRepository labelRepository;

    public ApplicantController(JobRepository jobRepository,
                            JobApplicationRepository applicationRepository,
                            MessageRepository messageRepository,
                            NotificationRepository notificationRepository,
                            ApplicantRepository applicantRepository,
                            ApplicantLabelRepository applicantLabelRepository,
                            LabelRepository labelRepository) {
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.messageRepository = messageRepository;
        this.notificationRepository = notificationRepository;
        this.applicantRepository = applicantRepository;
        this.applicantLabelRepository = applicantLabelRepository;
        this.labelRepository = labelRepository;
    }

    // True when a text has something written in it
    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    @GetMapping("/api/messages")
    @ResponseBody
    public List<Message> getMessages() {
        return messageRepository.findAll();
    }

    @PostMapping("/api/messages")
    @ResponseBody
    public Message sendMessage(@RequestBody Message message) {
        message.setCreatedAt("Today");
        message.setIsRead(true);
        return messageRepository.save(message);
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/login.html";
    }

    @PostMapping("/api/jobs")
    @ResponseBody
    public Job createJob(@RequestBody Job job) {
        return jobRepository.save(job);
    }

    @PostMapping("/api/applications/apply")
    @ResponseBody
    public ResponseEntity<?> applyForJob(@RequestParam String jobTitle, @RequestParam String company, HttpSession session) {
        // The logged-in applicant comes from the session
        Long applicantId = (Long) session.getAttribute("applicantId");
        if (applicantId == null) {
            return ResponseEntity.status(401).body("Please log in first.");
        }

        // Only look at this applicant's own applications
        List<Application> existing = applicationRepository.findByApplicantId(applicantId);
        for (Application a : existing) {
            if (jobTitle.equals(a.getJobTitle()) && company.equals(a.getCompany())) {
                return ResponseEntity.ok(a);
            }
        }
        

        Application app = new Application();
        app.setApplicantId(applicantId);
        app.setJobTitle(jobTitle);
        app.setCompany(company);
        app.setStatus("Under review");
        app.setAppliedOn("Oct 11, 2026");
        return ResponseEntity.ok(applicationRepository.save(app));
    }

    @GetMapping("/api/applications")
    @ResponseBody
    public ResponseEntity<?> getMyApplications(HttpSession session) {
        // Only the logged-in applicant's applications
        Long applicantId = (Long) session.getAttribute("applicantId");
        if (applicantId == null) {
            return ResponseEntity.status(401).body("Please log in first.");
        }
        return ResponseEntity.ok(applicationRepository.findByApplicantId(applicantId));
    }

    @GetMapping("/api/notifications")
    @ResponseBody
    public List<Notification> getNotifications() {
        return notificationRepository.findAll();
    }

    @PostMapping("/api/notifications/mark-read")
    @ResponseBody
    public Map<String, String> markAllNotificationsRead() {
        List<Notification> list = notificationRepository.findAll();
        for (Notification n : list) {
            n.setIsRead(true);
        }
        notificationRepository.saveAll(list);
        Map<String, String> res = new HashMap<>();
        res.put("status", "success");
        return res;
    }

    @GetMapping("/api/applicant/me")
    @ResponseBody
    public ResponseEntity<?> getApplicantInfo(HttpSession session) {
        // The logged-in applicant comes from the session
        Long applicantId = (Long) session.getAttribute("applicantId");
        if (applicantId == null) {
            return ResponseEntity.status(401).body("Please log in first.");
        }
        Applicant applicant = applicantRepository.findById(applicantId).orElse(null);
        if (applicant == null) {
            return ResponseEntity.status(401).body("Applicant not found. Please log in again.");
        }

        String fullName = applicant.getFullName() != null ? applicant.getFullName() : "";
        String email = applicant.getEmail() != null ? applicant.getEmail() : "";

        // Check which label groups this applicant ticked
        boolean hasSkill = false;
        boolean hasWorkSetup = false;
        List<Long> labelIds = new ArrayList<>();
        for (ApplicantLabel al : applicantLabelRepository.findByApplicantId(applicantId)) {
            labelIds.add(al.getLabelId());
        }
        for (Label l : labelRepository.findAllById(labelIds)) {
            if ("SKILL".equals(l.getLabelGroup())) hasSkill = true;
            if ("WORK_SETUP".equals(l.getLabelGroup())) hasWorkSetup = true;
        }
    

        // 7 checks, each worth the same
        int done = 0;
        if (hasText(fullName) && !fullName.equals("New Applicant")) done++;
        if (hasText(email)) done++;
        if (hasText(applicant.getRegion()) && hasText(applicant.getProvince()) && hasText(applicant.getCity())) done++;
        if (applicant.getEducationRank() != null) done++;
        if (applicant.getExperienceYears() != null) done++;
        if (hasSkill) done++;
        if (hasWorkSetup) done++;
        int profileStrength = Math.round(done * 100f / 7);

        Map<String, Object> response = new HashMap<>();
        response.put("fullName", fullName);
        response.put("email", email);
        response.put("profileStrength", profileStrength);
        response.put("activeApplications", applicationRepository.findByApplicantId(applicantId).size());
        response.put("savedJobs", 0);
        response.put("unreadMessages", messageRepository.count());
        response.put("upcomingInterviews", 0);
        response.put("unreadNotifications", notificationRepository.count());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dashboard")
    public String applicantDashboard() {
        return "forward:/dashboard.html";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "forward:/login.html";
    }
}