package com.emirio.config;

import com.emirio.user.Role;
import com.emirio.user.User;
import com.emirio.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

@Configuration
public class AdminSeeder {

  @Bean
  CommandLineRunner seedAdmin(UserRepository users, PasswordEncoder encoder) {
    return args -> {
      String email = "admin@emirio.tn";
      if (users.existsByEmail(email)) return;

      User admin = User.builder()
        .nom("Admin")
        .prenom("Emirio")
        .email(email)
        .mdp(encoder.encode("admin123"))   // change later
        .role(Role.ADMIN)
        .dateDeCreation(Instant.now())
        .statutCompte("ACTIVE")
        .build();

      users.save(admin);
      System.out.println("Admin created: " + email + " / admin123");
    };
  }
}
