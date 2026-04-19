package com.emirio.config;

import com.emirio.user.Role;
import com.emirio.user.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.List;

@Configuration
public class RoleSeeder {

    @Bean
    @Order(1)
    CommandLineRunner seedRoles(RoleRepository roleRepository) {
        return args -> {
            List<String> roleNames = List.of(
                    "ADMIN_GENERAL",
                    "USER",
                    "VENDEUR",
                    "CONTROLEUR"
            );

            for (String roleName : roleNames) {
                if (!roleRepository.existsByName(roleName)) {
                    roleRepository.save(Role.builder().name(roleName).build());
                }
            }
        };
    }
}