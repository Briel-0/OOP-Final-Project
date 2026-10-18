package com.workmatch.applicant;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ApplicantLabelRepository extends JpaRepository<ApplicantLabel, Long> {
    List<ApplicantLabel> findByApplicantId(Long applicantId);
    void deleteByApplicantId(Long applicantId);
}