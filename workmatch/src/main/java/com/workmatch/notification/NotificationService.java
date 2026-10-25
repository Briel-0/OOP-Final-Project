package com.workmatch.notification;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    // Sends a notification to one applicant
    public void notifyApplicant(Long applicantId, String title, String message, String targetPage) {
        if (applicantId == null) return;
        Notification n = new Notification();
        n.setApplicantId(applicantId);
        save(n, title, message, targetPage);
    }

    // Sends a notification to one company
    public void notifyCompany(String companyName, String title, String message, String targetPage) {
        if (companyName == null) return;
        Notification n = new Notification();
        n.setCompanyName(companyName);
        save(n, title, message, targetPage);
    }

    // Fills the shared fields and saves
    private void save(Notification n, String title, String message, String targetPage) {
        n.setTitle(title);
        n.setMessage(message);
        n.setTargetPage(targetPage);
        n.setSentAt(LocalDateTime.now());
        n.setCreatedAt(LocalDateTime.now().toString());
        n.setIsRead(false);
        notificationRepository.save(n);
    }
}