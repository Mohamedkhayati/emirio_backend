package com.emirio.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserRepository users;

    public ProfileController(UserRepository users) {
        this.users = users;
    }

    @GetMapping
    public UserProfileDto me(Authentication auth) {
        var email = auth.getName();
        var u = users.findByEmail(email).orElseThrow();
        return UserProfileDto.from(u);
    }

    @PutMapping
    public UserProfileDto update(Authentication auth, @RequestBody UpdateProfileRequest req) {
        var email = auth.getName();
        var u = users.findByEmail(email).orElseThrow();

        if (req.getNom() != null) u.setNom(req.getNom());
        if (req.getPrenom() != null) u.setPrenom(req.getPrenom());

        users.save(u);
        return UserProfileDto.from(u);
    }

    @Data
    public static class UpdateProfileRequest {
        private String nom;
        private String prenom;
    }

    @Data
    public static class UserProfileDto {
        private Long id;
        private String nom;
        private String prenom;
        private String email;
        private String role;
        private String statutCompte;

        public static UserProfileDto from(User u) {
            var dto = new UserProfileDto();
            dto.id = u.getId();
            dto.nom = u.getNom();
            dto.prenom = u.getPrenom();
            dto.email = u.getEmail();
            dto.role = u.getRole().name();
            dto.statutCompte = u.getStatutCompte();
            return dto;
        }
    }
}
