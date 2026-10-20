package com.workmatch.interview;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InterviewRepository extends JpaRepository<Interview, Long> {
    // One interview per application
    Optional<Interview> findFirstByApplicationId(Long applicationId);
}