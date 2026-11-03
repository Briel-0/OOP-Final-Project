package com.workmatch.practice;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PracticeQuestionRepository extends JpaRepository<PracticeQuestion, Long> {
    // All questions in the order they are asked
    List<PracticeQuestion> findAllByOrderByQuestionOrderAsc();
}