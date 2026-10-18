package com.workmatch.applicant;

import com.workmatch.job.Job;
import com.workmatch.job.JobRepository;
import com.workmatch.message.Message;
import com.workmatch.message.MessageRepository;
import com.workmatch.notification.Notification;
import com.workmatch.notification.NotificationRepository;

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

    public ApplicantController(JobRepository jobRepository,
                              JobApplicationRepository applicationRepository,
                              MessageRepository messageRepository,
                              NotificationRepository notificationRepository,
                              ApplicantRepository applicantRepository) {
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.messageRepository = messageRepository;
        this.notificationRepository = notificationRepository;
        this.applicantRepository = applicantRepository;
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
    public Application applyForJob(@RequestParam String jobTitle, @RequestParam String company) {
        List<Application> existing = applicationRepository.findAll();
        for (Application a : existing) {
            if (jobTitle.equals(a.getJobTitle()) && company.equals(a.getCompany())) {
                return a;
            }
        }

        Application app = new Application();
        app.setApplicantId(1L);
        app.setJobTitle(jobTitle);
        app.setCompany(company);
        app.setStatus("Under review");
        app.setAppliedOn("Oct 11, 2026");
        return applicationRepository.save(app);
    }

    @GetMapping("/api/applications")
    @ResponseBody
    public List<Application> getMyApplications() {
        return applicationRepository.findAll();
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
    public Map<String, Object> getApplicantInfo() {
        Map<String, Object> response = new HashMap<>();

        List<Applicant> applicants = applicantRepository.findAll();
        String fullName = "";
        String email = "";

        if (!applicants.isEmpty()) {
            Applicant latest = applicants.get(applicants.size() - 1);
            if (latest.getFullName() != null) fullName = latest.getFullName();
            if (latest.getEmail() != null) email = latest.getEmail();
        }

        int profileStrength = 0;
        if (!fullName.isEmpty()) profileStrength += 25;
        if (!email.isEmpty()) profileStrength += 25;
        if (applicationRepository.count() > 0) profileStrength += 25;
        if (jobRepository.count() > 0) profileStrength += 25;

        response.put("fullName", fullName.isEmpty() ? "User" : fullName);
        response.put("email", email.isEmpty() ? "user@example.com" : email);
        response.put("profileStrength", profileStrength);
        response.put("activeApplications", applicationRepository.count());
        response.put("savedJobs", 0);
        response.put("unreadMessages", messageRepository.count());
        response.put("upcomingInterviews", 0);
        response.put("unreadNotifications", notificationRepository.count());
        return response;
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