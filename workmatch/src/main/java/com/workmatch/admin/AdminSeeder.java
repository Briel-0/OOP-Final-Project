package com.workmatch.admin;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class AdminSeeder implements CommandLineRunner {

    private final AdminRepository adminRepository;

    public AdminSeeder(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    // Creates the admin account once if it does not exist yet
    @Override
    public void run(String... args) {
        if (adminRepository.findFirstByEmail("admin@workmatch.ph") == null) {
            Admin a = new Admin();
            a.setFullName("Mara Santos");
            a.setEmail("admin@workmatch.ph");
            a.setPassword("admin123");
            a.setCreatedAt(LocalDateTime.now());
            adminRepository.save(a);
        }
    }
}