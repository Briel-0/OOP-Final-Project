package com.workmatch.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // One applicant's notifications, newest first
    List<Notification> findByApplicantIdOrderBySentAtDesc(Long applicantId);

    // One company's notifications, newest first
    List<Notification> findByCompanyNameOrderBySentAtDesc(String companyName);

    // How many unread ones each side has
    long countByApplicantIdAndIsReadFalse(Long applicantId);
    long countByCompanyNameAndIsReadFalse(String companyName);
}