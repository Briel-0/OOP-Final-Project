package com.workmatch.notification;

import com.workmatch.applicant.Applicant;
import com.workmatch.applicant.ApplicantRepository;
import com.workmatch.applicant.Application;
import com.workmatch.applicant.JobApplicationRepository;
import com.workmatch.interview.Interview;
import com.workmatch.interview.InterviewRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ReminderService {

    private final InterviewRepository interviewRepository;
    private final JobApplicationRepository applicationRepository;
    private final ApplicantRepository applicantRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    public ReminderService(InterviewRepository interviewRepository,
                           JobApplicationRepository applicationRepository,
                           ApplicantRepository applicantRepository,
                           NotificationRepository notificationRepository,
                           NotificationService notificationService) {
        this.interviewRepository = interviewRepository;
        this.applicationRepository = applicationRepository;
        this.applicantRepository = applicantRepository;
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
    }

    // True when this application has a confirmed interview happening tomorrow
    private Interview tomorrowsInterview(Application a) {
        if (!"Interview scheduled".equals(a.getStatus())) return null;
        Interview i = interviewRepository.findFirstByApplicationId(a.getId()).orElse(null);
        if (i == null || i.getScheduledAt() == null) return null;
        if (!i.getScheduledAt().toLocalDate().equals(LocalDate.now().plusDays(1))) return null;
        return i;
    }

    // Reminders for one applicant (the day before the interview)
    public void interviewRemindersForApplicant(Long applicantId) {
        for (Application a : applicationRepository.findByApplicantId(applicantId)) {
            Interview i = tomorrowsInterview(a);
            if (i == null) continue;

            String title = "Interview tomorrow";
            String message = "Your interview for " + a.getJobTitle() + " at " + a.getCompany()
                    + " is on " + i.getScheduledAt().toString().replace('T', ' ') + ".";
            if (!notificationRepository.existsByApplicantIdAndTitleAndMessage(applicantId, title, message)) {
                notificationService.notifyApplicant(applicantId, title, message, "messages");
            }
        }
    }

    // Reminders for one company (the day before the interview)
    public void interviewRemindersForCompany(String companyName) {
        for (Application a : applicationRepository.findByCompany(companyName)) {
            Interview i = tomorrowsInterview(a);
            if (i == null) continue;

            Applicant applicant = a.getApplicantId() != null
                    ? applicantRepository.findById(a.getApplicantId()).orElse(null)
                    : null;
            String name = applicant != null ? applicant.getFullName() : "An applicant";

            String title = "Interview tomorrow";
            String message = "Interview with " + name + " for " + a.getJobTitle()
                    + " is on " + i.getScheduledAt().toString().replace('T', ' ') + ".";
            if (!notificationRepository.existsByCompanyNameAndTitleAndMessage(companyName, title, message)) {
                notificationService.notifyCompany(companyName, title, message, "interviews");
            }
        }
    }
}