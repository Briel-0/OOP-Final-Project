package com.workmatch.user;

import com.workmatch.applicant.Applicant;
import com.workmatch.applicant.ApplicantRepository;
import com.workmatch.company.CompanyRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final ApplicantRepository applicantRepository;
    private final CompanyRepository companyRepository;

    @Autowired
    public AuthController(ApplicantRepository applicantRepository, CompanyRepository companyRepository) {
        this.applicantRepository = applicantRepository;
        this.companyRepository = companyRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerApplicant(@RequestBody Map<String, String> payload) {
        try {
            String email = payload.get("email");
            String password = payload.get("password");
            String fullName = payload.get("fullName");

            if (email == null || password == null || email.isBlank() || password.isBlank()) {
                return ResponseEntity.badRequest().body("Email and password are required");
            }
            if (applicantRepository.findFirstByEmail(email) != null) {
                return ResponseEntity.status(409).body("Email already registered");
            }

            Applicant applicant = new Applicant();
            applicant.setEmail(email);
            applicant.setPassword(password);
            applicant.setFullName(fullName != null && !fullName.isEmpty() ? fullName : "New Applicant");
            applicantRepository.save(applicant);

            return ResponseEntity.ok().body("Registered successfully");
        } catch (Exception e) {
            e.printStackTrace(); // Logs exact error to terminal
            return ResponseEntity.status(400).body("Registration failed: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginApplicant(@RequestBody Map<String, String> payload, HttpSession session) {
        try {
            String email = payload.get("email");
            String password = payload.get("password");

            Applicant applicant = applicantRepository.findFirstByEmail(email);
            if (applicant != null && applicant.getPassword() != null
                    && applicant.getPassword().equals(password)) {
                session.setAttribute("applicantEmail", applicant.getEmail());
                session.setAttribute("applicantId", applicant.getId());
                return ResponseEntity.ok().body("/dashboard.html");
            }
            return ResponseEntity.status(401).body("Invalid credentials");
        } catch (Exception e) {
            e.printStackTrace(); // Logs exact error to terminal
            return ResponseEntity.status(500).body("Login error: " + e.getMessage());
        }
    }
}