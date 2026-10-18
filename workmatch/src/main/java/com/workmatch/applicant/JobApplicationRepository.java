package com.workmatch.applicant;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JobApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByApplicantId(Long applicantId);
    long countByApplicantIdAndStatus(Long applicantId, String status);
}