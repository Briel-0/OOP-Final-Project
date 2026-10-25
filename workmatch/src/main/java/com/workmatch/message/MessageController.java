package com.workmatch.message;

import com.workmatch.applicant.Applicant;
import com.workmatch.applicant.ApplicantRepository;
import com.workmatch.applicant.Application;
import com.workmatch.applicant.JobApplicationRepository;
import com.workmatch.notification.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/chat")
public class MessageController {

    private final MessageRepository messageRepository;
    private final JobApplicationRepository applicationRepository;
    private final ApplicantRepository applicantRepository;
    private final NotificationService notificationService;

    public MessageController(MessageRepository messageRepository,
                            JobApplicationRepository applicationRepository,
                            ApplicantRepository applicantRepository,
                            NotificationService notificationService) {
        this.messageRepository = messageRepository;
        this.applicationRepository = applicationRepository;
        this.applicantRepository = applicantRepository;
        this.notificationService = notificationService;
    }

    // True when this applicant applied to a job of this company
    private boolean hasApplied(Long applicantId, String companyName) {
        for (Application a : applicationRepository.findByApplicantId(applicantId)) {
            if (companyName != null && companyName.equals(a.getCompany())) {
                return true;
            }
        }
        return false;
    }

    // One row for the conversation list: name, last message, date, unread count
    private Map<String, Object> buildRow(String name, List<Message> chat, String otherRole) {
        int unread = 0;
        for (Message m : chat) {
            if (otherRole.equals(m.getSenderRole()) && !Boolean.TRUE.equals(m.getIsRead())) {
                unread++;
            }
        }
        Map<String, Object> row = new HashMap<>();
        row.put("name", name);
        row.put("unread", unread);
        if (chat.isEmpty()) {
            row.put("lastText", "");
            row.put("lastAt", null);
        } else {
            Message last = chat.get(chat.size() - 1);
            row.put("lastText", last.getMessageText());
            row.put("lastAt", last.getSentAt() != null ? last.getSentAt().toString() : null);
        }
        return row;
    }

    // Gets one chat and marks the other side's messages as read
    private List<Message> openChat(Long applicantId, String companyName, String otherRole) {
        List<Message> chat = messageRepository
                .findByApplicantIdAndCompanyNameOrderBySentAtAsc(applicantId, companyName);
        List<Message> changed = new ArrayList<>();
        for (Message m : chat) {
            if (otherRole.equals(m.getSenderRole()) && !Boolean.TRUE.equals(m.getIsRead())) {
                m.setIsRead(true);
                changed.add(m);
            }
        }
        messageRepository.saveAll(changed);
        return chat;
    }

    // Saves a new message
    private Message saveMessage(Long applicantId, String companyName, String role,
                                String senderName, String text) {
        Message m = new Message();
        m.setApplicantId(applicantId);
        m.setCompanyName(companyName);
        m.setSenderRole(role);
        m.setSenderName(senderName);
        m.setMessageText(text);
        m.setSentAt(LocalDateTime.now());
        m.setCreatedAt(LocalDateTime.now().toString());
        m.setIsRead(false);
        return messageRepository.save(m);
    }

    // ---------- APPLICANT SIDE ----------

    // Companies this applicant can chat with (the ones they applied to)
    @GetMapping("/applicant/conversations")
    public ResponseEntity<?> applicantConversations(HttpSession session) {
        Long applicantId = (Long) session.getAttribute("applicantId");
        if (applicantId == null) {
            return ResponseEntity.status(401).body("Please log in first.");
        }
        Set<String> companies = new LinkedHashSet<>();
        for (Application a : applicationRepository.findByApplicantId(applicantId)) {
            if (a.getCompany() != null) companies.add(a.getCompany());
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (String company : companies) {
            List<Message> chat = messageRepository
                    .findByApplicantIdAndCompanyNameOrderBySentAtAsc(applicantId, company);
            result.add(buildRow(company, chat, "COMPANY"));
        }
        return ResponseEntity.ok(result);
    }

    // The chat with one company
    @GetMapping("/applicant/messages")
    public ResponseEntity<?> applicantMessages(@RequestParam String company, HttpSession session) {
        Long applicantId = (Long) session.getAttribute("applicantId");
        if (applicantId == null) {
            return ResponseEntity.status(401).body("Please log in first.");
        }
        if (!hasApplied(applicantId, company)) {
            return ResponseEntity.status(403).body("You can only message companies you applied to.");
        }
        return ResponseEntity.ok(openChat(applicantId, company, "COMPANY"));
    }

    // Applicant sends a message to a company
    @PostMapping("/applicant/send")
    public ResponseEntity<?> applicantSend(@RequestBody Map<String, String> payload, HttpSession session) {
        Long applicantId = (Long) session.getAttribute("applicantId");
        if (applicantId == null) {
            return ResponseEntity.status(401).body("Please log in first.");
        }
        String company = payload.get("company");
        String text = payload.get("text");
        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().body("Message cannot be empty.");
        }
        if (company == null || !hasApplied(applicantId, company)) {
            return ResponseEntity.status(403).body("You can only message companies you applied to.");
        }
        Applicant applicant = applicantRepository.findById(applicantId).orElse(null);
        String name = applicant != null ? applicant.getFullName() : "Applicant";
        return ResponseEntity.ok(saveMessage(applicantId, company, "APPLICANT", name, text.trim()));
    }

    // Applicant confirms the interview time on a schedule card
    @PostMapping("/applicant/card/{messageId}/confirm")
    public ResponseEntity<?> confirmInterview(@PathVariable Long messageId, HttpSession session) {
        Long applicantId = (Long) session.getAttribute("applicantId");
        if (applicantId == null) {
            return ResponseEntity.status(401).body("Please log in first.");
        }
        Message card = messageRepository.findById(messageId).orElse(null);
        if (card == null || !applicantId.equals(card.getApplicantId()) || !"SCHEDULE".equals(card.getMessageType())) {
            return ResponseEntity.status(404).body("Card not found.");
        }
        if (!"Pending".equals(card.getCardStatus())) {
            return ResponseEntity.badRequest().body("This card is no longer open.");
        }
        Application a = applicationRepository.findById(card.getApplicationId()).orElse(null);
        if (a == null || !"Interview invited".equals(a.getStatus())) {
            return ResponseEntity.badRequest().body("This interview can no longer be confirmed.");
        }

        a.setStatus("Interview scheduled");
        applicationRepository.save(a);
        card.setCardStatus("Confirmed");
        messageRepository.save(card);

        // Tell the company the applicant confirmed the time
        Applicant applicant = applicantRepository.findById(applicantId).orElse(null);
        String name = applicant != null ? applicant.getFullName() : "An applicant";
        notificationService.notifyCompany(
                a.getCompany(),
                "Interview time confirmed",
                name + " confirmed the interview for " + a.getJobTitle(),
                "interviews");

        return ResponseEntity.ok().body("Interview confirmed");
    }

    // Applicant says they cannot make the time
    @PostMapping("/applicant/card/{messageId}/decline")
    public ResponseEntity<?> declineInterview(@PathVariable Long messageId, HttpSession session) {
        Long applicantId = (Long) session.getAttribute("applicantId");
        if (applicantId == null) {
            return ResponseEntity.status(401).body("Please log in first.");
        }
        Message card = messageRepository.findById(messageId).orElse(null);
        if (card == null || !applicantId.equals(card.getApplicantId()) || !"SCHEDULE".equals(card.getMessageType())) {
            return ResponseEntity.status(404).body("Card not found.");
        }
        if (!"Pending".equals(card.getCardStatus())) {
            return ResponseEntity.badRequest().body("This card is no longer open.");
        }

        // The status stays Interview invited so the company can pick a new time
        card.setCardStatus("Declined");
        messageRepository.save(card);

        // Tell the company the applicant cannot make the time
        Application a = applicationRepository.findById(card.getApplicationId()).orElse(null);
        if (a != null) {
            Applicant applicant = applicantRepository.findById(applicantId).orElse(null);
            String name = applicant != null ? applicant.getFullName() : "An applicant";
            notificationService.notifyCompany(
                    a.getCompany(),
                    "Applicant can't make the time",
                    name + " can't make the interview for " + a.getJobTitle() + ". Please pick a new time.",
                    "interviews");   
        }

        return ResponseEntity.ok().body("Marked as can't make it");
    }

   // Applicant accepts the job offer on an offer card
    @PostMapping("/applicant/offer/{messageId}/accept")
    public ResponseEntity<?> acceptOffer(@PathVariable Long messageId, HttpSession session) {
        Long applicantId = (Long) session.getAttribute("applicantId");
        if (applicantId == null) {
            return ResponseEntity.status(401).body("Please log in first.");
        }
        Message card = messageRepository.findById(messageId).orElse(null);
        if (card == null || !applicantId.equals(card.getApplicantId()) || !"OFFER".equals(card.getMessageType())) {
            return ResponseEntity.status(404).body("Offer not found.");
        }
        if (!"Pending".equals(card.getCardStatus())) {
            return ResponseEntity.badRequest().body("This offer is no longer open.");
        }
        Application a = applicationRepository.findById(card.getApplicationId()).orElse(null);
        if (a == null || !"Offer sent".equals(a.getStatus())) {
            return ResponseEntity.badRequest().body("This offer can no longer be accepted.");
        }

        // The applicant is hired
        a.setStatus("Hired");
        applicationRepository.save(a);
        card.setCardStatus("Accepted");
        messageRepository.save(card);

        // Tell the company the applicant accepted the offer
        Applicant applicant = applicantRepository.findById(applicantId).orElse(null);
        String name = applicant != null ? applicant.getFullName() : "An applicant";
        notificationService.notifyCompany(
                a.getCompany(),
                "Offer accepted",
                name + " accepted your offer for " + a.getJobTitle(),
                "candidates");

        // One hire per job: every other open applicant on the same job is Not selected
        for (Application other : applicationRepository.findByCompany(a.getCompany())) {
            if (other.getId().equals(a.getId())) continue;
            if ("Hired".equals(other.getStatus()) || "Not selected".equals(other.getStatus())) continue;

            // Use the job id when both rows have one, otherwise the job title
            boolean sameJob;
            if (a.getJobId() != null && other.getJobId() != null) {
            sameJob = a.getJobId().equals(other.getJobId());
            } else {
                sameJob = a.getJobTitle() != null && a.getJobTitle().equals(other.getJobTitle());
            }
            if (sameJob) {
                other.setStatus("Not selected");
                other.setRejectionReason("Position has been filled");
                applicationRepository.save(other);
            }
        }
        return ResponseEntity.ok().body("Offer accepted");
    }

    // Applicant declines the job offer
    @PostMapping("/applicant/offer/{messageId}/decline")
    public ResponseEntity<?> declineOffer(@PathVariable Long messageId, HttpSession session) {
        Long applicantId = (Long) session.getAttribute("applicantId");
        if (applicantId == null) {
            return ResponseEntity.status(401).body("Please log in first.");
        }
        Message card = messageRepository.findById(messageId).orElse(null);
        if (card == null || !applicantId.equals(card.getApplicantId()) || !"OFFER".equals(card.getMessageType())) {
            return ResponseEntity.status(404).body("Offer not found.");
        }
        if (!"Pending".equals(card.getCardStatus())) {
            return ResponseEntity.badRequest().body("This offer is no longer open.");
        }
        Application a = applicationRepository.findById(card.getApplicationId()).orElse(null);
        if (a == null || !"Offer sent".equals(a.getStatus())) {
            return ResponseEntity.badRequest().body("This offer can no longer be declined.");
        }

        a.setStatus("Not selected");
        a.setRejectionReason("Applicant declined the offer");
        applicationRepository.save(a);
        card.setCardStatus("Declined");
        messageRepository.save(card);

        // Tell the company the applicant declined the offer
        Applicant applicant = applicantRepository.findById(applicantId).orElse(null);
        String name = applicant != null ? applicant.getFullName() : "An applicant";
        notificationService.notifyCompany(
                a.getCompany(),
                "Offer declined",
                name + " declined your offer for " + a.getJobTitle(),
                "candidates");

        return ResponseEntity.ok().body("Offer declined");
    }

    // Company sends a message to an applicant
    @PostMapping("/company/send")
    public ResponseEntity<?> companySend(@RequestBody Map<String, String> payload, HttpSession session) {
        String companyName = (String) session.getAttribute("companyName");
        if (companyName == null) {
            return ResponseEntity.status(401).body("Please log in first.");
        }
        String text = payload.get("text");
        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().body("Message cannot be empty.");
        }
        Long applicantId;
        try {
            applicantId = Long.valueOf(payload.get("applicantId"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Applicant is required.");
        }
        if (!hasApplied(applicantId, companyName)) {
            return ResponseEntity.status(403).body("This applicant has not applied to your jobs.");
        }
        return ResponseEntity.ok(saveMessage(applicantId, companyName, "COMPANY", companyName, text.trim()));
    }
}