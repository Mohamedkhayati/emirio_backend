package com.emirio.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Past;
import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserRepository users;

    public ProfileController(UserRepository users) {
        this.users = users;
    }

    @GetMapping
    public UserProfileDto me(Authentication auth) {
        var u = getCurrentUser(auth);
        return UserProfileDto.from(u);
    }

    @PutMapping
    public UserProfileDto update(Authentication auth, @Valid @RequestBody UpdateProfileRequest req) {
        var u = getCurrentUser(auth);

        if (req.getNom() != null && !req.getNom().isBlank()) u.setNom(req.getNom().trim());
        if (req.getPrenom() != null && !req.getPrenom().isBlank()) u.setPrenom(req.getPrenom().trim());
        if (req.getDateNaissance() != null) u.setDateNaissance(req.getDateNaissance());
        if (req.getSexe() != null) u.setSexe(req.getSexe());

        u.setProfileCompleted(isProfileCompleted(u));
        users.save(u);

        return UserProfileDto.from(u);
    }

    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserProfileDto uploadPhoto(Authentication auth, @RequestParam("photo") MultipartFile photo) throws IOException {
        var u = getCurrentUser(auth);

        if (photo == null || photo.isEmpty()) throw new IllegalArgumentException("Photo is required");

        var contentType = photo.getContentType();
        if (contentType == null ||
                !(contentType.equals("image/jpeg")
                        || contentType.equals("image/png")
                        || contentType.equals("image/webp"))) {
            throw new IllegalArgumentException("Only JPG, PNG or WEBP images are allowed");
        }

        if (photo.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Photo must be 5 MB or less");
        }

        u.setPhotoName(photo.getOriginalFilename());
        u.setPhotoType(contentType);
        u.setPhotoData(photo.getBytes());

        users.save(u);
        return UserProfileDto.from(u);
    }

    @GetMapping("/user-photo/{userId}")
    public ResponseEntity<byte[]> getUserPhoto(@PathVariable Long userId) {
        User u = users.findById(userId).orElseThrow();

        if (u.getPhotoData() == null || u.getPhotoData().length == 0) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            if (u.getPhotoType() != null && !u.getPhotoType().isBlank()) {
                mediaType = MediaType.parseMediaType(u.getPhotoType());
            }
        } catch (Exception ignored) {
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(u.getPhotoData());
    }

    @GetMapping("/photo")
    public ResponseEntity<byte[]> getPhoto(Authentication auth) {
        var u = getCurrentUser(auth);

        if (u.getPhotoData() == null || u.getPhotoData().length == 0) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            if (u.getPhotoType() != null && !u.getPhotoType().isBlank()) {
                mediaType = MediaType.parseMediaType(u.getPhotoType());
            }
        } catch (Exception ignored) {
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .body(u.getPhotoData());
    }

    private User getCurrentUser(Authentication auth) {
        return users.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    private boolean isProfileCompleted(User u) {
        return u.getDateNaissance() != null && u.getSexe() != null;
    }

    @Data
    public static class UpdateProfileRequest {
        private String nom;
        private String prenom;

        @Past
        private LocalDate dateNaissance;

        private Gender sexe;
    }

    @Data
    public static class UserProfileDto {
        private Long id;
        private String nom;
        private String prenom;
        private String email;
        private String role;
        private String statutCompte;
        private LocalDate dateNaissance;
        private Integer age;
        private String sexe;
        private boolean profileCompleted;
        private boolean hasPhoto;

        public static UserProfileDto from(User u) {
            var dto = new UserProfileDto();
            dto.id = u.getId();
            dto.nom = u.getNom();
            dto.prenom = u.getPrenom();
            dto.email = u.getEmail();
            dto.role = u.getRole() != null ? u.getRole().getName() : null;
            dto.statutCompte = u.getStatutCompte();
            dto.dateNaissance = u.getDateNaissance();
            dto.age = u.getDateNaissance() == null ? null : Period.between(u.getDateNaissance(), LocalDate.now()).getYears();
            dto.sexe = u.getSexe() == null ? null : u.getSexe().name();
            dto.profileCompleted = u.isProfileCompleted();
            dto.hasPhoto = u.getPhotoData() != null && u.getPhotoData().length > 0;
            return dto;
        }
    }
}