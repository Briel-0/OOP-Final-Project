package com.workmatch.label;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.workmatch.Label;
import com.workmatch.LabelRepository;
import com.workmatch.Location;
import com.workmatch.LocationRepository;

import java.util.Arrays;

@Configuration
public class ReferenceDataLoader {

    @Bean
    CommandLineRunner initDatabase(LabelRepository labelRepository, LocationRepository locationRepository) {
        return args -> {
            if (labelRepository.count() == 0) {
                // Skills (mustHave false)
                String[] skills = {
                    "Computer literacy", "Basic MS Office (Word/Excel/PowerPoint)", "Typing", 
                    "Data entry", "Documentation", "Communication (written)", "Communication (verbal)", 
                    "Customer service", "Email and chat support", "Problem solving", 
                    "Organization and scheduling", "Teamwork", "Social media or content", 
                    "Manual or software testing", "HTML and CSS"
                };
                for (String s : skills) {
                    labelRepository.save(new Label(s, "SKILL", false, null));
                }

                // Physical Demands (mustHave true)
                String[] physicalDemands = {
                    "Prolonged sitting", "Prolonged standing", "Walking or traveling", 
                    "Climbing stairs", "Bending or stooping", "Reaching overhead", 
                    "Lifting light items (up to 5 kg)", "Lifting heavy items (over 5 kg)", 
                    "Repetitive hand movements", "Fine hand work (typing or writing)", 
                    "Driving", "Phone calls or frequent speaking", 
                    "Face-to-face customer interaction", "Long screen time"
                };
                for (String pd : physicalDemands) {
                    labelRepository.save(new Label(pd, "PHYSICAL_DEMAND", true, null));
                }

                // Accessibility (mustHave true)
                String[] accTrue = {
                    "Wheelchair-accessible entrance and workspace", "Accessible restroom", 
                    "Ground floor or elevator access", "Assistive tools (voice-to-text or adaptive keyboard)", 
                    "Screen reader compatible tools", "Sign language interpreter"
                };
                for (String a : accTrue) {
                    labelRepository.save(new Label(a, "ACCESSIBILITY", true, null));
                }

                // Accessibility (mustHave false)
                String[] accFalse = {
                    "Flexible breaks", "Flexible schedule", "Adjustable or ergonomic workstation", 
                    "Captions or written meeting notes", "Text-based communication (chat or email instead of calls)", 
                    "Written task instructions", "Transport or parking assistance", "Extra time for tasks or training"
                };
                for (String a : accFalse) {
                    labelRepository.save(new Label(a, "ACCESSIBILITY", false, null));
                }

                // Work Setup (mustHave false)
                String[] workSetups = {
                    "On-site", "Remote", "Hybrid", "Full-time", "Part-time", 
                    "Project-based", "Contractual", "Internship or OJT", "Day shift", "Night shift"
                };
                for (String ws : workSetups) {
                    labelRepository.save(new Label(ws, "WORK_SETUP", false, null));
                }

                // Education (mustHave false, rankOrder 1 to 9)
                String[] educations = {
                    "No formal requirement", "Elementary graduate", "High school graduate", 
                    "Senior high school graduate", "Vocational or TESDA certificate", 
                    "College level", "Associate or 2-year course", "Bachelor's degree", "Master's degree"
                };
                for (int i = 0; i < educations.length; i++) {
                    labelRepository.save(new Label(educations[i], "EDUCATION", false, i + 1));
                }
            }

            if (locationRepository.count() == 0) {
                // Locations
                Object[][] locs = {
                    {"NCR", "Metro Manila", "Manila"},
                    {"NCR", "Metro Manila", "Quezon City"},
                    {"NCR", "Metro Manila", "Caloocan"},
                    {"NCR", "Metro Manila", "Makati"},
                    {"NCR", "Metro Manila", "Pasig"},
                    {"NCR", "Metro Manila", "Taguig"},
                    {"NCR", "Metro Manila", "Mandaluyong"},
                    {"NCR", "Metro Manila", "Marikina"},
                    {"Calabarzon", "Cavite", "Bacoor"},
                    {"Calabarzon", "Cavite", "Dasmariñas"},
                    {"Calabarzon", "Cavite", "Imus"},
                    {"Calabarzon", "Laguna", "Calamba"},
                    {"Calabarzon", "Laguna", "Santa Rosa"},
                    {"Calabarzon", "Laguna", "San Pedro"},
                    {"Calabarzon", "Rizal", "Antipolo"},
                    {"Calabarzon", "Rizal", "Cainta"},
                    {"Calabarzon", "Rizal", "Taytay"},
                    {"Calabarzon", "Batangas", "Batangas City"},
                    {"Calabarzon", "Batangas", "Lipa"}
                };

                for (Object[] l : locs) {
                    locationRepository.save(new Location((String) l[0], (String) l[1], (String) l[2]));
                }
            }
        };
    }
}