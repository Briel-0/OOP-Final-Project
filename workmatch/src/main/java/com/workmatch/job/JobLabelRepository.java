package com.workmatch.job;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JobLabelRepository extends JpaRepository<JobLabel, Long> {
    List<JobLabel> findByJobId(Long jobId);
    void deleteByJobId(Long jobId);
}