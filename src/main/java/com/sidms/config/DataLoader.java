package com.sidms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.sidms.entity.MemberProfile;
import com.sidms.entity.Role;
import com.sidms.entity.User;
import com.sidms.repository.MemberProfileRepository;
import com.sidms.repository.UserRepository;
import com.sidms.util.EncryptionService;

/**
 * Seeds the database with sample users and profiles on first run.
 * Skips if data already exists (idempotent).
 */
@Configuration
public class DataLoader {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    @SuppressWarnings("unused")
    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository,
                                   MemberProfileRepository profileRepository,
                                   PasswordEncoder passwordEncoder,
                                   EncryptionService encryptionService) {
        return args -> {
            if (userRepository.count() > 0) {
                log.info("Database already seeded — skipping data loader.");
                return;
            }

            log.info("Seeding database with sample data...");

            // ---- Users ----
            userRepository.save(User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("Admin@123"))
                    .role(Role.ROLE_ADMIN)
                    .mfaEnabled(true)
                    .enabled(true)
                    .build());

            userRepository.save(User.builder()
                    .username("manager1")
                    .password(passwordEncoder.encode("Manager@123"))
                    .role(Role.ROLE_MANAGER)
                    .mfaEnabled(true)
                    .enabled(true)
                    .build());

            User member = userRepository.save(User.builder()
                    .username("member1")
                    .password(passwordEncoder.encode("Member@123"))
                    .role(Role.ROLE_MEMBER)
                    .mfaEnabled(true)
                    .enabled(true)
                    .build());

            log.info("Created users: admin, manager1, member1");

            // ---- Member Profiles ----
            profileRepository.save(MemberProfile.builder()
                    .fullName("John Doe")
                    .email("john.doe@example.com")
                    .phoneNumber(encryptionService.encrypt("+1-555-0101"))
                    .address(encryptionService.encrypt("123 Main St, Springfield, IL 62704"))
                    .resumeUrl(encryptionService.encrypt("https://storage.example.com/resumes/john-doe.pdf"))
                    .governmentId(encryptionService.encrypt("SSN-XXX-XX-1234"))
                    .createdBy("admin")
                    .assignedManager("manager1")
                    .userId(member.getId())
                    .build());

            profileRepository.save(MemberProfile.builder()
                    .fullName("Jane Smith")
                    .email("jane.smith@example.com")
                    .phoneNumber(encryptionService.encrypt("+1-555-0202"))
                    .address(encryptionService.encrypt("456 Oak Ave, Portland, OR 97201"))
                    .resumeUrl(encryptionService.encrypt("https://storage.example.com/resumes/jane-smith.pdf"))
                    .governmentId(encryptionService.encrypt("SSN-XXX-XX-5678"))
                    .createdBy("admin")
                    .assignedManager("manager1")
                    .build());

            log.info("Created sample member profiles.");
            log.info("=== Data seeding complete ===");
            log.info("Login credentials:");
            log.info("  Admin   → admin / Admin@123");
            log.info("  Manager → manager1 / Manager@123");
            log.info("  Member  → member1 / Member@123");
        };
    }
}
