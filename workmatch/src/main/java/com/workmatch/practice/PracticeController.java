package com.workmatch.practice;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/practice")
public class PracticeController {

    private final PracticeQuestionRepository questionRepository;

    public PracticeController(PracticeQuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    // The practice questions, only for a logged-in applicant
    @GetMapping("/questions")
    public ResponseEntity<?> getQuestions(HttpSession session) {
        Long applicantId = (Long) session.getAttribute("applicantId");
        if (applicantId == null) {
            return ResponseEntity.status(401).body("Please log in first.");
        }
        return ResponseEntity.ok(questionRepository.findAllByOrderByQuestionOrderAsc());
    }
}