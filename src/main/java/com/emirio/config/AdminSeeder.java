package com.emirio.config;

import com.emirio.user.RoleRepository;
import com.emirio.user.User;
import com.emirio.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

@Configuration
public class AdminSeeder {

    @Bean
    @Order(2)
    CommandLineRunner seedAdmin(UserRepository users,
                                RoleRepository roles,
                                PasswordEncoder encoder) {
        return args -> {
            String email = "admin@emirio.tn";
            if (users.existsByEmailIgnoreCase(email)) return;

            var adminRole = roles.findByName("ADMIN_GENERAL")
                    .orElseThrow(() -> new IllegalStateException("Role ADMIN_GENERAL not found"));

            User admin = User.builder()
                    .nom("Admin")
                    .prenom("Emirio")
                    .email(email.toLowerCase())
                    .mdp(encoder.encode("admin123"))
                    .role(adminRole)
                    .dateDeCreation(Instant.now())
                    .statutCompte("ACTIVE")
                    .build();

            users.save(admin);
        };
    }
}