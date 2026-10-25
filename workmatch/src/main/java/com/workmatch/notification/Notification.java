package com.workmatch.notification;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String createdAt;
    private Boolean isRead = false;

    // Owner: an applicant notification fills applicantId, a company one fills companyName
    private Long applicantId;
    private String companyName;
    // The short sentence under the title (company page shows it)
    private String message;
    // Real date and time, used for "10 minutes ago"
    private LocalDateTime sentAt;
    // Where the View button goes: "candidates", "interviews", "messages" or "applications"
    private String targetPage;

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }
    public Long getApplicantId() { return applicantId; }
    public void setApplicantId(Long applicantId) { this.applicantId = applicantId; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public String getTargetPage() { return targetPage; }
    public void setTargetPage(String targetPage) { this.targetPage = targetPage; }
}