package com.workmatch.applicant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicantRepository extends JpaRepository<Applicant, Long> {
    Applicant findFirstByEmail(String email);
    Applicant findByEmail(String email);
}

