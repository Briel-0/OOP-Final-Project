package com.workmatch.interview;

import com.workmatch.applicant.Applicant;
import com.workmatch.applicant.ApplicantRepository;
import com.workmatch.applicant.Application;
import com.workmatch.applicant.JobApplicationRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/meeting")
public class MeetingController {

    // Our JaaS account id (not secret)
    private static final String JAAS_APP_ID = "vpaas-magic-cookie-384d489b6cf14b579942cacdde50c945";

    private final InterviewRepository interviewRepository;
    private final JobApplicationRepository applicationRepository;
    private final ApplicantRepository applicantRepository;

    public MeetingController(InterviewRepository interviewRepository,
                             JobApplicationRepository applicationRepository,
                             ApplicantRepository applicantRepository) {
        this.interviewRepository = interviewRepository;
        this.applicationRepository = applicationRepository;
        this.applicantRepository = applicantRepository;
    }

    // Tells the page if the logged-in person can join this interview now
    @GetMapping("/{applicationId}")
    public ResponseEntity<?> meetingInfo(@PathVariable Long applicationId, HttpSession session) {
        String companyName = (String) session.getAttribute("companyName");
        Long applicantId = (Long) session.getAttribute("applicantId");
        if (companyName == null && applicantId == null) {
            return ResponseEntity.status(401).body("Please log in first.");
        }

        // The application must exist and belong to this person
        Application a = applicationRepository.findById(applicationId).orElse(null);
        if (a == null) {
            return ResponseEntity.status(404).body("Meeting not found.");
        }
        boolean isCompany = companyName != null && companyName.equals(a.getCompany());
        boolean isApplicant = applicantId != null && applicantId.equals(a.getApplicantId());
        if (!isCompany && !isApplicant) {
            return ResponseEntity.status(404).body("Meeting not found.");
        }

        // The applicant must have confirmed the time first
        if (!"Interview scheduled".equals(a.getStatus())) {
            return ResponseEntity.badRequest().body("This interview is not confirmed yet.");
        }
        Interview interview = interviewRepository.findFirstByApplicationId(applicationId).orElse(null);
        if (interview == null || interview.getScheduledAt() == null) {
            return ResponseEntity.badRequest().body("No interview time is set.");
        }

        // Company opens 30 minutes before, applicant 5 minutes before, both close when it ends
        LocalDateTime start = interview.getScheduledAt();
        LocalDateTime end = start.plusMinutes(interview.getDurationMinutes());
        LocalDateTime opensAt = start.minusMinutes(isCompany ? 30 : 5);
        LocalDateTime now = LocalDateTime.now();
        boolean open = !now.isBefore(opensAt) && now.isBefore(end);

        Applicant applicant = a.getApplicantId() != null
                ? applicantRepository.findById(a.getApplicantId()).orElse(null)
                : null;
        String applicantName = applicant != null ? applicant.getFullName() : "Applicant";

        Map<String, Object> response = new HashMap<>();
        response.put("open", open);
        response.put("role", isCompany ? "COMPANY" : "APPLICANT");
        response.put("opensAt", opensAt.toString());
        response.put("startsAt", start.toString());
        response.put("endsAt", end.toString());
        response.put("jobTitle", a.getJobTitle());
        response.put("companyName", a.getCompany());
        response.put("applicantName", applicantName);
        response.put("displayName", isCompany ? a.getCompany() : applicantName);
        // The room is only given out while the meeting is open
        if (open) {
            response.put("appId", JAAS_APP_ID);
            response.put("roomName", "WorkMatch-Interview-" + applicationId);
        }
        return ResponseEntity.ok(response);
    }
}