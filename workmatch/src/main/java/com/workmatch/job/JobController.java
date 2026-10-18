package com.workmatch.job;

import com.workmatch.applicant.Applicant;
import com.workmatch.applicant.ApplicantRepository;
import com.workmatch.applicant.ApplicantLabel;
import com.workmatch.applicant.ApplicantLabelRepository;
import com.workmatch.Label;
import com.workmatch.matching.MatchingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.workmatch.LabelRepository;

import java.util.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api")
public class JobController {

    private final JobRepository jobRepository;
    private final JobLabelRepository jobLabelRepository;
    private final ApplicantRepository applicantRepository;
    private final ApplicantLabelRepository applicantLabelRepository;
    private final MatchingService matchingService;
    private final LabelRepository labelRepository;

        @Autowired
        public JobController(JobRepository jobRepository,
                     JobLabelRepository jobLabelRepository,
                     ApplicantRepository applicantRepository,
                     ApplicantLabelRepository applicantLabelRepository,
                     MatchingService matchingService,
                     LabelRepository labelRepository) {
    this.jobRepository = jobRepository;
    this.jobLabelRepository = jobLabelRepository;
    this.applicantRepository = applicantRepository;
    this.applicantLabelRepository = applicantLabelRepository;
    this.matchingService = matchingService;
    this.labelRepository = labelRepository;
}

    @GetMapping("/jobs")
    public ResponseEntity<?> getJobs(HttpSession session) {
        Long applicantId = (Long) session.getAttribute("applicantId");
        Applicant applicant = applicantId != null ? applicantRepository.findById(applicantId).orElse(null) : null;

        List<Label> applicantLabels = new ArrayList<>();
        if (applicant != null) {
            applicantLabels = loadApplicantLabels(applicant.getId());
        }

        List<Job> jobs = jobRepository.findAll();
        List<JobResponseDto> response = new ArrayList<>();
        for (Job job : jobs) {
            List<Label> jobLabels = loadJobLabels(job.getId());

            List<Long> labelIds = new ArrayList<>();
            for (Label l : jobLabels) {
                labelIds.add(l.getId());
            }

            // No logged-in applicant means no match number
            int matchPct = 0;
            if (applicant != null) {
                matchPct = matchingService.calculateMatch(applicant, job, jobLabels, applicantLabels).getScore();
            }

            JobResponseDto dto = new JobResponseDto();
            dto.setId(job.getId());
            dto.setTitle(job.getTitle());
            dto.setCompany(job.getCompany());
            dto.setLocation(job.getLocation());
            dto.setSalaryMin(job.getSalaryMin());
            dto.setSalaryMax(job.getSalaryMax());
            dto.setMinEducationRank(job.getMinEducationRank());
            dto.setExperienceRank(job.getExperienceRank());
            dto.setRegion(job.getRegion());
            dto.setProvince(job.getProvince());
            dto.setCity(job.getCity());
            dto.setOtherNotes(job.getOtherNotes());
            dto.setLabelIds(labelIds);
            dto.setMatchPct(matchPct);
            // Expired when the closing date is before today
            dto.setExpired(job.getClosingDate() != null && job.getClosingDate().isBefore(LocalDate.now()));
            dto.setClosingDate(job.getClosingDate() != null ? job.getClosingDate().toString() : null);
            response.add(dto);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/jobs/matches")
    @ResponseBody
    public ResponseEntity<?> getJobMatches(HttpSession session) {
        // The logged-in applicant always comes from the session
        Long applicantId = (Long) session.getAttribute("applicantId");
        if (applicantId == null) {
            return ResponseEntity.status(401).body("Please log in first.");
        }
        Applicant applicant = applicantRepository.findById(applicantId).orElse(null);
        if (applicant == null) {
            return ResponseEntity.status(401).body("Applicant not found. Please log in again.");
        }

        List<Label> applicantLabels = loadApplicantLabels(applicantId);
        List<Job> jobs = jobRepository.findAll();

        List<Map<String, Object>> results = new ArrayList<>();
        for (Job job : jobs) {
            List<Label> jobLabels = loadJobLabels(job.getId());
            MatchingService.MatchResult matchResult = matchingService.calculateMatch(applicant, job, jobLabels, applicantLabels);

            Map<String, Object> map = new HashMap<>();
            map.put("job", job);
            map.put("score", matchResult.getScore());
            map.put("level", matchResult.getLevel());
            map.put("pointsPerPart", matchResult.getPointsPerPart());
            map.put("lacking", matchResult.lacking());
            results.add(map);
        }

        // Highest score first
        results.sort((a, b) -> Integer.compare((int) b.get("score"), (int) a.get("score")));
        return ResponseEntity.ok(results);
    }

    // Gets the full labels the company ticked for one job
    private List<Label> loadJobLabels(Long jobId) {
        List<Long> ids = new ArrayList<>();
        for (JobLabel jl : jobLabelRepository.findByJobId(jobId)) {
            ids.add(jl.getLabelId());
        }
        return labelRepository.findAllById(ids);
    }

    // Gets the full labels one applicant ticked
    private List<Label> loadApplicantLabels(Long applicantId) {
        List<Long> ids = new ArrayList<>();
        for (ApplicantLabel al : applicantLabelRepository.findByApplicantId(applicantId)) {
            ids.add(al.getLabelId());
        }
        return labelRepository.findAllById(ids);
    }

    @PostMapping("/company/jobs")
    public ResponseEntity<?> createJob(@RequestBody JobRequestDto dto, HttpSession session) {
        String companyName = (String) session.getAttribute("companyName");
        if (companyName == null) {
            companyName = "Work4Us";
        }

        Job job = new Job();
        job.setTitle(dto.getTitle());
        job.setCompany(companyName);
        job.setLocation((dto.getCity() != null ? dto.getCity() : "Remote") + ", " + (dto.getProvince() != null ? dto.getProvince() : "Metro Manila"));
        job.setSalaryMin(dto.getSalaryMin());
        job.setSalaryMax(dto.getSalaryMax());
        job.setMinEducationRank(dto.getMinEducationRank());
        job.setExperienceRank(dto.getExperienceRank());
        job.setRegion(dto.getRegion());
        job.setProvince(dto.getProvince());
        job.setCity(dto.getCity());
        job.setOtherNotes(dto.getOtherNotes());

        // Closing date comes as text like 2026-11-30
        if (dto.getClosingDate() != null && !dto.getClosingDate().isBlank()) {
            job.setClosingDate(LocalDate.parse(dto.getClosingDate()));
        }

    Job savedJob = jobRepository.save(job);

    jobLabelRepository.deleteByJobId(savedJob.getId());
    if (dto.getLabelIds() != null) {
        for (Long labelId : dto.getLabelIds()) {
            jobLabelRepository.save(new JobLabel(savedJob.getId(), labelId));
        }
    }

    return ResponseEntity.ok().body("Job posted successfully");
}

    public static class JobRequestDto {
        private String title;
        private Double salaryMin;
        private Double salaryMax;
        private Integer minEducationRank;
        private Integer experienceRank;
        private String region;
        private String province;
        private String city;
        private String otherNotes;
        private String closingDate;
        private List<Long> labelIds;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public Double getSalaryMin() { return salaryMin; }
        public void setSalaryMin(Double salaryMin) { this.salaryMin = salaryMin; }
        public Double getSalaryMax() { return salaryMax; }
        public void setSalaryMax(Double salaryMax) { this.salaryMax = salaryMax; }
        public Integer getMinEducationRank() { return minEducationRank; }
        public void setMinEducationRank(Integer minEducationRank) { this.minEducationRank = minEducationRank; }
        public Integer getExperienceRank() { return experienceRank; }
        public void setExperienceRank(Integer experienceRank) { this.experienceRank = experienceRank; }
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public String getProvince() { return province; }
        public void setProvince(String province) { this.province = province; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getOtherNotes() { return otherNotes; }
        public void setOtherNotes(String otherNotes) { this.otherNotes = otherNotes; }
        public String getClosingDate() { return closingDate; }
        public void setClosingDate(String closingDate) { this.closingDate = closingDate; }
        public List<Long> getLabelIds() { return labelIds; }
        public void setLabelIds(List<Long> labelIds) { this.labelIds = labelIds; }
    }

    public static class JobResponseDto {
        private Long id;
        private String title;
        private String company;
        private String location;
        private Double salaryMin;
        private Double salaryMax;
        private Integer minEducationRank;
        private Integer experienceRank;
        private String region;
        private String province;
        private String city;
        private String otherNotes;
        private List<Long> labelIds;
        private int matchPct;
        private boolean expired;
        private String closingDate;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getCompany() { return company; }
        public void setCompany(String company) { this.company = company; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public Double getSalaryMin() { return salaryMin; }
        public void setSalaryMin(Double salaryMin) { this.salaryMin = salaryMin; }
        public Double getSalaryMax() { return salaryMax; }
        public void setSalaryMax(Double salaryMax) { this.salaryMax = salaryMax; }
        public Integer getMinEducationRank() { return minEducationRank; }
        public void setMinEducationRank(Integer minEducationRank) { this.minEducationRank = minEducationRank; }
        public Integer getExperienceRank() { return experienceRank; }
        public void setExperienceRank(Integer experienceRank) { this.experienceRank = experienceRank; }
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public String getProvince() { return province; }
        public void setProvince(String province) { this.province = province; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getOtherNotes() { return otherNotes; }
        public void setOtherNotes(String otherNotes) { this.otherNotes = otherNotes; }
        public List<Long> getLabelIds() { return labelIds; }
        public void setLabelIds(List<Long> labelIds) { this.labelIds = labelIds; }
        public int getMatchPct() { return matchPct; }
        public void setMatchPct(int matchPct) { this.matchPct = matchPct; }
        public boolean isExpired() { return expired; }
        public void setExpired(boolean expired) { this.expired = expired; }
        public String getClosingDate() { return closingDate; }
        public void setClosingDate(String closingDate) { this.closingDate = closingDate; }
    }
}