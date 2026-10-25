package com.workmatch.interview;

import com.workmatch.applicant.Applicant;
import com.workmatch.applicant.ApplicantRepository;
import com.workmatch.applicant.Application;
import com.workmatch.applicant.JobApplicationRepository;
import com.workmatch.message.Message;
import com.workmatch.message.MessageRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api/company/interviews")
public class InterviewController {

    private final InterviewRepository interviewRepository;
    private final JobApplicationRepository applicationRepository;
    private final ApplicantRepository applicantRepository;
    private final MessageRepository messageRepository;

    public InterviewController(InterviewRepository interviewRepository,
                               JobApplicationRepository applicationRepository,
                               ApplicantRepository applicantRepository,
                               MessageRepository messageRepository) {
        this.interviewRepository = interviewRepository;
        this.applicationRepository = applicationRepository;
        this.applicantRepository = applicantRepository;
        this.messageRepository = messageRepository;
    }

    // Applicants the company accepted, with their interview time if one is set
    @GetMapping
    public ResponseEntity<?> listInterviews(HttpSession session) {
        String companyName = (String) session.getAttribute("companyName");
        if (companyName == null) {
            return ResponseEntity.status(401).body("Please log in first.");
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Application a : applicationRepository.findByCompany(companyName)) {
            if (!"Interview invited".equals(a.getStatus()) && !"Interview scheduled".equals(a.getStatus())) {
                continue;
            }
            Applicant applicant = a.getApplicantId() != null
                    ? applicantRepository.findById(a.getApplicantId()).orElse(null)
                    : null;
            Interview interview = interviewRepository.findFirstByApplicationId(a.getId()).orElse(null);

            Map<String, Object> row = new HashMap<>();
            row.put("applicationId", a.getId());
            row.put("applicantName", applicant != null ? applicant.getFullName() : "Unknown");
            row.put("jobTitle", a.getJobTitle());
            row.put("status", a.getStatus());
            row.put("scheduledAt", interview != null ? interview.getScheduledAt().toString() : null);
            row.put("durationMinutes", interview != null ? interview.getDurationMinutes() : null);
            result.add(row);
        }
        return ResponseEntity.ok(result);
    }

    // Sets or changes the interview time for one application
    @PostMapping("/{applicationId}/schedule")
    public ResponseEntity<?> schedule(@PathVariable Long applicationId,
                                      @RequestBody Map<String, String> payload,
                                      HttpSession session) {
        String companyName = (String) session.getAttribute("companyName");
        if (companyName == null) {
            return ResponseEntity.status(401).body("Please log in first.");
        }

        // The application must exist, belong to this company, and be accepted
        Application a = applicationRepository.findById(applicationId).orElse(null);
        if (a == null || !companyName.equals(a.getCompany())) {
            return ResponseEntity.status(404).body("Application not found.");
        }
        if (!"Interview invited".equals(a.getStatus()) && !"Interview scheduled".equals(a.getStatus())) {
            return ResponseEntity.badRequest().body("This applicant is not in the interview stage.");
        }

        // Read the date, time and length
        LocalDateTime when;
        int minutes;
        try {
            LocalDate date = LocalDate.parse(payload.get("date"));
            LocalTime time = LocalTime.parse(payload.get("time"));
            when = LocalDateTime.of(date, time);
            minutes = Integer.parseInt(payload.get("duration"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Please give a valid date, time and length.");
        }
        if (minutes != 30 && minutes != 45 && minutes != 60) {
            return ResponseEntity.badRequest().body("Length must be 30, 45 or 60 minutes.");
        }
        if (!when.isAfter(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("The interview must be in the future.");
        }

        // Update the old interview or make a new one
        Interview interview = interviewRepository.findFirstByApplicationId(applicationId).orElse(new Interview());
        interview.setApplicationId(applicationId);
        interview.setScheduledAt(when);
        interview.setDurationMinutes(minutes);
        interviewRepository.save(interview);

        // A new or changed time must be confirmed again by the applicant
        a.setStatus("Interview invited");
        applicationRepository.save(a);

        // Send the schedule to the applicant as a card in Messages
        if (a.getApplicantId() != null) {
            // Older cards for this application are no longer valid
            for (Message old : messageRepository.findByApplicantIdAndCompanyNameOrderBySentAtAsc(a.getApplicantId(), a.getCompany())) {
                if ("SCHEDULE".equals(old.getMessageType()) && applicationId.equals(old.getApplicationId())
                        && "Pending".equals(old.getCardStatus())) {
                    old.setCardStatus("Replaced");
                    messageRepository.save(old);
                }
            }

            Message card = new Message();
            card.setApplicantId(a.getApplicantId());
            card.setCompanyName(a.getCompany());
            card.setSenderRole("COMPANY");
            card.setSenderName(a.getCompany());
            card.setMessageType("SCHEDULE");
            card.setApplicationId(applicationId);
            card.setCardStatus("Pending");
            card.setMessageText("Interview for " + a.getJobTitle() + ": "
                    + when.toString().replace('T', ' ') + " (" + minutes + " minutes)");
            card.setSentAt(LocalDateTime.now());
            card.setCreatedAt(LocalDateTime.now().toString());
            card.setIsRead(false);
            messageRepository.save(card);
        }

        return ResponseEntity.ok().body("Interview scheduled");
    }

    // Company sends a job offer after the interview time has passed
    @PostMapping("/{applicationId}/offer")
    public ResponseEntity<?> sendOffer(@PathVariable Long applicationId, HttpSession session) {
        String companyName = (String) session.getAttribute("companyName");
        if (companyName == null) {
            return ResponseEntity.status(401).body("Please log in first.");
        }

        // The application must exist and belong to this company
        Application a = applicationRepository.findById(applicationId).orElse(null);
        if (a == null || !companyName.equals(a.getCompany())) {
            return ResponseEntity.status(404).body("Application not found.");
        }

        // Only an applicant whose interview time was confirmed can get an offer
        if (!"Interview scheduled".equals(a.getStatus())) {
            return ResponseEntity.badRequest().body("Only a confirmed interview can get an offer.");
        }

        // The interview must be over
        Interview interview = interviewRepository.findFirstByApplicationId(applicationId).orElse(null);
        if (interview == null || interview.getScheduledAt() == null) {
            return ResponseEntity.badRequest().body("No interview time is set.");
        }
        LocalDateTime end = interview.getScheduledAt().plusMinutes(interview.getDurationMinutes());
        if (LocalDateTime.now().isBefore(end)) {
            return ResponseEntity.badRequest().body("The interview has not ended yet.");
        }

        a.setStatus("Offer sent");
        applicationRepository.save(a);

        // Send the offer to the applicant as a card in Messages
        if (a.getApplicantId() != null) {
            Message card = new Message();
            card.setApplicantId(a.getApplicantId());
            card.setCompanyName(a.getCompany());
            card.setSenderRole("COMPANY");
            card.setSenderName(a.getCompany());
            card.setMessageType("OFFER");
            card.setApplicationId(applicationId);
            card.setCardStatus("Pending");
            card.setMessageText("Job offer for " + a.getJobTitle() + " at " + a.getCompany());
            card.setSentAt(LocalDateTime.now());
            card.setCreatedAt(LocalDateTime.now().toString());
            card.setIsRead(false);
            messageRepository.save(card);
        }

        return ResponseEntity.ok().body("Offer sent");
    }
}