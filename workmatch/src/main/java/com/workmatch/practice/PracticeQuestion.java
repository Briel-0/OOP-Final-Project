package com.workmatch.practice;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "practice_questions")
public class PracticeQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1, 2, 3: the order the questions are asked
    private Integer questionOrder;
    private String questionText;
    // The answering tip shown under the question
    private String tip;

    public PracticeQuestion() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getQuestionOrder() { return questionOrder; }
    public void setQuestionOrder(Integer questionOrder) { this.questionOrder = questionOrder; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getTip() { return tip; }
    public void setTip(String tip) { this.tip = tip; }
}