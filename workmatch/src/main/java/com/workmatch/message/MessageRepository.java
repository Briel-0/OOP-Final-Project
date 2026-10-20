package com.workmatch.message;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    // One chat between one applicant and one company, oldest first
    List<Message> findByApplicantIdAndCompanyNameOrderBySentAtAsc(Long applicantId, String companyName);

    // How many messages sent by one side (COMPANY or APPLICANT) this applicant has not read yet
    long countByApplicantIdAndSenderRoleAndIsReadFalse(Long applicantId, String senderRole);
}