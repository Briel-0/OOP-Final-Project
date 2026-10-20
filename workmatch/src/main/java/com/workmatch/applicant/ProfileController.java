package com.workmatch.applicant;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/applicant")
public class ProfileController {

    private final ApplicantRepository applicantRepository;
    private final ApplicantLabelRepository applicantLabelRepository;

    public ProfileController(ApplicantRepository applicantRepository, ApplicantLabelRepository applicantLabelRepository) {
        this.applicantRepository = applicantRepository;
        this.applicantLabelRepository = applicantLabelRepository;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(HttpSession session) {
        Long applicantId = (Long) session.getAttribute("applicantId");
        if (applicantId == null) {
            return ResponseEntity.status(401).body("Not logged in");
        }

        Applicant applicant = applicantRepository.findById(applicantId).orElse(null);
        if (applicant == null) return ResponseEntity.notFound().build();

        List<Long> labelIds = applicantLabelRepository.findByApplicantId(applicantId)
                .stream().map(ApplicantLabel::getLabelId).collect(Collectors.toList());

        ProfileDto dto = new ProfileDto();
        dto.setFullName(applicant.getFullName());
        dto.setEmail(applicant.getEmail());
        dto.setEducationRank(applicant.getEducationRank());
        dto.setExperienceYears(applicant.getExperienceYears());
        dto.setRegion(applicant.getRegion());
        dto.setProvince(applicant.getProvince());
        dto.setCity(applicant.getCity());
        dto.setOtherNeeds(applicant.getOtherNeeds());
        dto.setProgram(applicant.getProgram());
        dto.setLabelIds(labelIds);

        return ResponseEntity.ok(dto);
    }

    @PostMapping("/profile")
    public ResponseEntity<?> saveProfile(@RequestBody ProfileDto dto, HttpSession session) {
        Long applicantId = (Long) session.getAttribute("applicantId");
        if (applicantId == null) {
            return ResponseEntity.status(401).body("Not logged in");
        }

        Applicant applicant = applicantRepository.findById(applicantId).orElse(null);
        if (applicant == null) return ResponseEntity.notFound().build();

        if (dto.getFullName() != null) applicant.setFullName(dto.getFullName());
        if (dto.getEmail() != null) applicant.setEmail(dto.getEmail());
        applicant.setEducationRank(dto.getEducationRank());
        applicant.setExperienceYears(dto.getExperienceYears());
        applicant.setRegion(dto.getRegion());
        applicant.setProvince(dto.getProvince());
        applicant.setCity(dto.getCity());
        applicant.setOtherNeeds(dto.getOtherNeeds());
        applicant.setProgram(dto.getProgram());
        applicantRepository.save(applicant);

        // Save selected label checkboxes
        applicantLabelRepository.deleteAll(applicantLabelRepository.findByApplicantId(applicantId));
        if (dto.getLabelIds() != null) {
            for (Long labelId : dto.getLabelIds()) {
                applicantLabelRepository.save(new ApplicantLabel(applicantId, labelId));
            }
        }

        return ResponseEntity.ok().body("Profile saved successfully");
    }

    public static class ProfileDto {
        private String fullName;
        private String email;
        private Integer educationRank;
        private Double experienceYears;
        private String region;
        private String province;
        private String city;
        private String otherNeeds;
        private String program;
        private List<Long> labelIds;

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public Integer getEducationRank() { return educationRank; }
        public void setEducationRank(Integer educationRank) { this.educationRank = educationRank; }
        public Double getExperienceYears() { return experienceYears; }
        public void setExperienceYears(Double experienceYears) { this.experienceYears = experienceYears; }
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public String getProvince() { return province; }
        public void setProvince(String province) { this.province = province; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getOtherNeeds() { return otherNeeds; }
        public void setOtherNeeds(String otherNeeds) { this.otherNeeds = otherNeeds; }
        public String getProgram() { return program; }
        public void setProgram(String program) { this.program = program; }
        public List<Long> getLabelIds() { return labelIds; }
        public void setLabelIds(List<Long> labelIds) { this.labelIds = labelIds; }
    }
}