package com.workmatch.applicant;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/applicant/resume")
public class ResumeController {

    // Folder where the PDF files are saved (inside the project folder)
    private static final Path RESUME_FOLDER = Paths.get("uploads", "resumes");

    private final ApplicantRepository applicantRepository;

    public ResumeController(ApplicantRepository applicantRepository) {
        this.applicantRepository = applicantRepository;
    }

    // Where one applicant's PDF is saved (the company side will use this too)
    public static Path fileFor(Long applicantId) {
        return RESUME_FOLDER.resolve("resume-" + applicantId + ".pdf");
    }

    // Upload or replace the résumé
    @PostMapping
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file, HttpSession session) {
        Long applicantId = (Long) session.getAttribute("applicantId");
        if (applicantId == null) {
            return ResponseEntity.status(401).body("Please log in first.");
        }
        Applicant applicant = applicantRepository.findById(applicantId).orElse(null);
        if (applicant == null) {
            return ResponseEntity.status(401).body("Applicant not found. Please log in again.");
        }
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please choose a PDF file.");
        }

        try {
            byte[] bytes = file.getBytes();
            // A real PDF file always starts with %PDF
            if (bytes.length < 4 || bytes[0] != '%' || bytes[1] != 'P' || bytes[2] != 'D' || bytes[3] != 'F') {
                return ResponseEntity.badRequest().body("Only PDF files are allowed.");
            }
            Files.createDirectories(RESUME_FOLDER);
            Files.write(fileFor(applicantId), bytes);

            String name = file.getOriginalFilename();
            applicant.setResumeFileName(name != null && !name.isBlank() ? name : "Resume.pdf");
            applicantRepository.save(applicant);
            return ResponseEntity.ok().body("Resume saved");
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Could not save the file.");
        }
    }

    // Tells the page if this applicant has a résumé and its name
    @GetMapping("/info")
    public ResponseEntity<?> info(HttpSession session) {
        Long applicantId = (Long) session.getAttribute("applicantId");
        if (applicantId == null) {
            return ResponseEntity.status(401).body("Please log in first.");
        }
        Applicant applicant = applicantRepository.findById(applicantId).orElse(null);
        if (applicant == null) {
            return ResponseEntity.status(401).body("Applicant not found. Please log in again.");
        }
        boolean has = applicant.getResumeFileName() != null && Files.exists(fileFor(applicantId));
        Map<String, Object> response = new HashMap<>();
        response.put("hasResume", has);
        response.put("fileName", has ? applicant.getResumeFileName() : null);
        return ResponseEntity.ok(response);
    }

    // Opens the PDF itself (the Open button)
    @GetMapping
    public ResponseEntity<?> open(HttpSession session) {
        Long applicantId = (Long) session.getAttribute("applicantId");
        if (applicantId == null) {
            return ResponseEntity.status(401).body("Please log in first.");
        }
        Path file = fileFor(applicantId);
        if (!Files.exists(file)) {
            return ResponseEntity.status(404).body("No resume uploaded yet.");
        }
        try {
            byte[] bytes = Files.readAllBytes(file);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"resume.pdf\"")
                    .body(bytes);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Could not read the file.");
        }
    }
}