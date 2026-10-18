package com.workmatch.matching;

import com.workmatch.applicant.Applicant;
import com.workmatch.job.Job;
import com.workmatch.Label;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MatchingService {

    public static class MatchResult {
        private int score;
        private String level;
        private Map<String, Double> pointsPerPart;
        private List<String> lacking;

        public MatchResult(int score, String level, Map<String, Double> pointsPerPart, List<String> lacking) {
            this.score = score;
            this.level = level;
            this.pointsPerPart = pointsPerPart;
            this.lacking = lacking;
        }

        public int getScore() { return score; }
        public String getLevel() { return level; }
        public Map<String, Double> getPointsPerPart() { return pointsPerPart; }
        public List<String> lacking() { return lacking; }
        public List<String> getLacking() { return lacking; }
    }

    public MatchResult calculateMatch(Applicant applicant, Job job, List<Label> jobLabels, List<Label> applicantLabels) {
    Map<String, Double> pointsPerPart = new LinkedHashMap<>();
    List<LackingItem> lackingItems = new ArrayList<>();

    // Remote = has Remote and does NOT have On-site or Hybrid
    boolean isRemote = jobLabels.stream().anyMatch(l -> l.getName().equalsIgnoreCase("Remote"))
            && jobLabels.stream().noneMatch(l -> l.getName().equalsIgnoreCase("On-site") || l.getName().equalsIgnoreCase("Hybrid"));

    Set<Long> applicantLabelIds = applicantLabels.stream().map(Label::getId).collect(Collectors.toSet());
    Set<Long> jobLabelIds = jobLabels.stream().map(Label::getId).collect(Collectors.toSet());

    // 1. Skills (30)
    List<Label> jobSkills = jobLabels.stream().filter(l -> "SKILL".equals(l.getLabelGroup())).collect(Collectors.toList());
    double skillPts = 30.0;
    if (!jobSkills.isEmpty()) {
        long matched = jobSkills.stream().filter(l -> applicantLabelIds.contains(l.getId())).count();
        skillPts = ((double) matched / jobSkills.size()) * 30.0;
        for (Label l : jobSkills) {
            if (!applicantLabelIds.contains(l.getId())) {
                lackingItems.add(new LackingItem(false, "Missing skill: " + l.getName()));
            }
        }
    }
    pointsPerPart.put("Skills", skillPts);

    // 2. Physical demands (15): applicant ticks what they CANNOT do
    List<Label> jobDemands = jobLabels.stream().filter(l -> "PHYSICAL_DEMAND".equals(l.getLabelGroup())).collect(Collectors.toList());
    double demandPts = 15.0;
    boolean demandCap = false;
    if (!jobDemands.isEmpty()) {
        long canDo = jobDemands.stream().filter(l -> !applicantLabelIds.contains(l.getId())).count();
        demandPts = ((double) canDo / jobDemands.size()) * 15.0;
        for (Label l : jobDemands) {
            if (applicantLabelIds.contains(l.getId())) {
                demandCap = true;
                lackingItems.add(new LackingItem(true, "Physical demand conflict: " + l.getName() + " (cannot do)"));
            }
        }
    }
    pointsPerPart.put("Physical Demands", demandPts);

    // 3. Accessibility needs (15)
    List<Label> needs = applicantLabels.stream().filter(l -> "ACCESSIBILITY".equals(l.getLabelGroup())).collect(Collectors.toList());
    double accessPts = 15.0;
    boolean accessCap = false;
    Set<String> remoteAutoOffered = Set.of(
            "Wheelchair-accessible entrance and workspace",
            "Accessible restroom",
            "Ground floor or elevator access",
            "Transport or parking assistance");
    if (!needs.isEmpty()) {
        long met = 0;
        for (Label need : needs) {
            boolean offered = jobLabelIds.contains(need.getId())
                    || (isRemote && remoteAutoOffered.contains(need.getName()));
            if (offered) {
                met++;
            } else {
                boolean mustHave = need.isMustHave();
                if (mustHave) accessCap = true;
                lackingItems.add(new LackingItem(mustHave, "Needs " + need.getName() + ": not offered"));
            }
        }
        accessPts = ((double) met / needs.size()) * 15.0;
    }
    pointsPerPart.put("Accessibility Needs", accessPts);

    // 4. Work setup (10)
    List<Label> jobSetups = jobLabels.stream().filter(l -> "WORK_SETUP".equals(l.getLabelGroup())).collect(Collectors.toList());
    double setupPts = 10.0;
    if (!jobSetups.isEmpty()) {
        long matched = jobSetups.stream().filter(l -> applicantLabelIds.contains(l.getId())).count();
        setupPts = ((double) matched / jobSetups.size()) * 10.0;
        for (Label l : jobSetups) {
            if (!applicantLabelIds.contains(l.getId())) {
                lackingItems.add(new LackingItem(false, "Work setup: missing " + l.getName()));
            }
        }
    }
    pointsPerPart.put("Work Setup", setupPts);

    // 5. Education (10)
    int jobEduRank = job.getMinEducationRank() != null ? job.getMinEducationRank() : 0;
    int appEduRank = applicant.getEducationRank() != null ? applicant.getEducationRank() : 0;
    int eduDiff = jobEduRank - appEduRank;
    double eduPts = 10.0;
    if (eduDiff == 1) eduPts = 6.0;
    else if (eduDiff == 2) eduPts = 3.0;
    else if (eduDiff >= 3) eduPts = 0.0;
    if (eduDiff >= 1) {
        lackingItems.add(new LackingItem(false, "Education: job needs a higher degree level"));
    }
    pointsPerPart.put("Education", eduPts);

    // 6. Experience (10): turn years into a rank first
    double appYears = applicant.getExperienceYears() != null ? applicant.getExperienceYears() : 0.0;
    int appExpRank = 1;                          // under 1 year
    if (appYears > 5) appExpRank = 4;            // over 5 years
    else if (appYears >= 3) appExpRank = 3;      // 3 up to 5
    else if (appYears >= 1) appExpRank = 2;      // 1 up to under 3

    int jobExpRank = job.getExperienceRank() != null ? job.getExperienceRank() : 0;
    double expPts = 10.0;
    if (jobExpRank > 0) {
        int expDiff = jobExpRank - appExpRank;
        if (expDiff == 1) expPts = 7.0;
        else if (expDiff == 2) expPts = 5.0;
        else if (expDiff == 3) expPts = 2.0;
        else if (expDiff >= 4) expPts = 0.0;
        if (expDiff >= 1) {
            lackingItems.add(new LackingItem(false, "Experience: job requires more years of experience"));
        }
    }
    pointsPerPart.put("Experience", expPts);

    // 7. Location (10)
    double locPts = 10.0;
    if (!isRemote) {
        String jCity = job.getCity() != null ? job.getCity().trim().toLowerCase() : "";
        String aCity = applicant.getCity() != null ? applicant.getCity().trim().toLowerCase() : "";
        String jProv = job.getProvince() != null ? job.getProvince().trim().toLowerCase() : "";
        String aProv = applicant.getProvince() != null ? applicant.getProvince().trim().toLowerCase() : "";
        String jReg = job.getRegion() != null ? job.getRegion().trim().toLowerCase() : "";
        String aReg = applicant.getRegion() != null ? applicant.getRegion().trim().toLowerCase() : "";

        if (!jCity.isEmpty() && jCity.equals(aCity)) {
            locPts = 10.0;
        } else if (!jProv.isEmpty() && jProv.equals(aProv)) {
            locPts = 6.0;
            lackingItems.add(new LackingItem(false, "Location: different city"));
        } else if (!jReg.isEmpty() && jReg.equals(aReg)) {
            locPts = 3.0;
            lackingItems.add(new LackingItem(false, "Location: different province"));
        } else {
            locPts = 0.0;
            lackingItems.add(new LackingItem(false, "Location: different region"));
        }
    }
    pointsPerPart.put("Location", locPts);

    // Total and cap
    double total = skillPts + demandPts + accessPts + setupPts + eduPts + expPts + locPts;
    double cap = (accessCap || demandCap) ? 40.0 : 100.0;
    int finalScore = (int) Math.round(Math.min(total, cap));

    String level = "Not recommended";
    if (finalScore >= 80) level = "Excellent Match";
    else if (finalScore >= 60) level = "Good Match";
    else if (finalScore >= 40) level = "Fair Match";

    // Must-have items first (the sort keeps the original order inside each group)
    lackingItems.sort((a, b) -> Boolean.compare(b.isMustHave, a.isMustHave));
    List<String> lackingTexts = lackingItems.stream().map(item -> item.text).collect(Collectors.toList());

    return new MatchResult(finalScore, level, pointsPerPart, lackingTexts);
    }

    private static class LackingItem {
        boolean isMustHave;
        String text;
        public LackingItem(boolean isMustHave, String text) {
            this.isMustHave = isMustHave;
            this.text = text;
        }
    }
}