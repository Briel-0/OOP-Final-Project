package com.workmatch.practice;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class PracticeFeedbackController {

    // Words that usually show a real example
    private static final String[] EXAMPLE_WORDS = {
            "for example", "for instance", "once", "when i", "at my", "in my",
            "project", "experience", "last year", "during"};

    // Words that usually show a result or something learned
    private static final String[] RESULT_WORDS = {
            "result", "learned", "helped", "improved", "finished", "completed",
            "achieved", "solved", "succeeded", "because of"};

    // True when the text has any of the words (matches the start of a word)
    private boolean hasAny(String text, String[] words) {
        for (String w : words) {
            if (text.contains(" " + w)) return true;
        }
        return false;
    }

    // One line of feedback: passed or not, and a short sentence
    private Map<String, Object> check(boolean passed, String goodText, String badText) {
        Map<String, Object> item = new HashMap<>();
        item.put("passed", passed);
        item.put("message", passed ? goodText : badText);
        return item;
    }

    // Checks the answer with simple rules and sends back short feedback
    @PostMapping("/api/practice/feedback")
    public ResponseEntity<?> getFeedback(@RequestBody Map<String, String> payload, HttpSession session) {
        Long applicantId = (Long) session.getAttribute("applicantId");
        if (applicantId == null) {
            return ResponseEntity.status(401).body("Please log in first.");
        }
        String answer = payload.get("answer");
        if (answer == null || answer.isBlank()) {
            return ResponseEntity.badRequest().body("Please speak or type an answer first.");
        }

        // Lower case, no punctuation, one space on each end
        String text = " " + answer.toLowerCase().replaceAll("[^a-z0-9']+", " ").trim() + " ";
        int wordCount = answer.trim().split("\\s+").length;

        List<Map<String, Object>> checks = new ArrayList<>();

        // 1. Length
        boolean lengthOk = wordCount >= 25 && wordCount <= 200;
        String lengthBad = wordCount < 25
                ? "Your answer is a bit short. Try to say a few more sentences (about 25 words or more)."
                : "Your answer is long. Try to keep it under 200 words.";
        checks.add(check(lengthOk, "Good length for an interview answer.", lengthBad));

        // 2. Example
        checks.add(check(hasAny(text, EXAMPLE_WORDS),
                "You gave an example from your own experience.",
                "Add a real example, such as a time you did this at school, at work, or in a project."));

        // 3. Result or learning
        checks.add(check(hasAny(text, RESULT_WORDS),
                "You said what happened or what you learned.",
                "Say what the result was or what you learned."));

        // 4. Talks about themselves
        boolean aboutYou = text.contains(" i ") || text.contains(" my ")
                || text.contains(" i'm ") || text.contains(" i've ");
        checks.add(check(aboutYou,
                "You talked about yourself clearly.",
                "Use I and my so the employer hears about you, for example: I did, or my role was."));

        // Short summary line from how many checks passed
        int passedCount = 0;
        for (Map<String, Object> c : checks) {
            if ((boolean) c.get("passed")) passedCount++;
        }
        String summary;
        if (passedCount == 4) summary = "Great answer! You covered everything.";
        else if (passedCount == 3) summary = "Good answer. One thing to improve.";
        else if (passedCount == 2) summary = "Not bad. Two things to improve.";
        else summary = "A good start. Try the tips below.";

        Map<String, Object> response = new HashMap<>();
        response.put("summary", summary);
        response.put("checks", checks);
        return ResponseEntity.ok(response);
    }
}