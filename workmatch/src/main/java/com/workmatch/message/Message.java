package com.workmatch.message;

import jakarta.persistence.*;

@Entity
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String senderName;
    private String companyName;
    private String subject;
    private String messageText;
    private String createdAt;
    private Boolean isRead;
    // Which applicant this chat belongs to
    private Long applicantId;
    // Who wrote it: "APPLICANT" or "COMPANY"
    private String senderRole;
    // Real date and time it was sent
    private java.time.LocalDateTime sentAt;
    // "TEXT" for normal chat, "SCHEDULE" for an interview card (old rows are empty, treat as TEXT)
    private String messageType;
    // Which application a card is about
    private Long applicationId;
    // For cards: Pending, Confirmed, Declined or Replaced
    private String cardStatus;

    // Getters and Setters
    public Long getId() { return id; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getMessageText() { return messageText; }
    public void setMessageText(String messageText) { this.messageText = messageText; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }
    public Long getApplicantId() { return applicantId; }
    public void setApplicantId(Long applicantId) { this.applicantId = applicantId; }
    public String getSenderRole() { return senderRole; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }
    public java.time.LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(java.time.LocalDateTime sentAt) { this.sentAt = sentAt; }
    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
    public String getCardStatus() { return cardStatus; }
    public void setCardStatus(String cardStatus) { this.cardStatus = cardStatus; }
}