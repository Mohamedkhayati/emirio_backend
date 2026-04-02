package com.emirio.admin;

import com.emirio.user.Role;
import com.emirio.user.User;
import com.emirio.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AdminController {

    private static final String MAIN_ADMIN_EMAIL = "admin.general@emirio.tn";

    private final UserRepository users;

    public AdminController(UserRepository users) {
        this.users = users;
    }

    @GetMapping({"/users", "/clients"})
    public List<UserDto> listUsers() {
        return users.findAll().stream()
            .sorted((a, b) -> {
                Instant da = a.getDateDeCreation();
                Instant db = b.getDateDeCreation();
                if (da == null && db == null) return 0;
                if (da == null) return 1;
                if (db == null) return -1;
                return db.compareTo(da);
            })
            .map(UserDto::from)
            .toList();
    }

    @GetMapping({"/users/{id}", "/clients/{id}"})
    public UserDto getUser(@PathVariable Long id) {
        return UserDto.from(requireUser(id));
    }

    @PutMapping({"/users/{id}/status", "/clients/{id}/status"})
    public UserDto updateStatus(@PathVariable Long id, @Valid @RequestBody StatusReq req) {
        User u = requireUser(id);
        protectMainAdmin(u);

        u.setStatutCompte(normalizeStatus(req.getStatutCompte()));
        users.save(u);

        return UserDto.from(u);
    }

    @PutMapping("/users/{id}/role")
    public UserDto updateRole(@PathVariable Long id, @Valid @RequestBody RoleReq req) {
        User u = requireUser(id);
        protectMainAdmin(u);

        Role role;
        try {
            role = Role.valueOf(req.getRole().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role");
        }

        u.setRole(role);
        users.save(u);

        return UserDto.from(u);
    }

    @DeleteMapping({"/users/{id}", "/clients/{id}"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        User u = requireUser(id);
        protectMainAdmin(u);
        users.deleteById(id);
    }

    private User requireUser(Long id) {
        return users.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void protectMainAdmin(User u) {
        if (u.getEmail() != null && MAIN_ADMIN_EMAIL.equalsIgnoreCase(u.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Main admin account cannot be changed");
        }
    }

    private String normalizeStatus(String status) {
        String value = status == null ? "" : status.trim().toUpperCase();

        if (!value.equals("ACTIVE") && !value.equals("BLOCKED") && !value.equals("DISABLED")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid account status");
        }

        return value;
    }

    @Data
    public static class StatusReq {
        @NotBlank
        private String statutCompte;
    }

    @Data
    public static class RoleReq {
        @NotBlank
        private String role;
    }

    @Data
    public static class UserDto {
        private Long id;
        private String nom;
        private String prenom;
        private String email;
        private String role;
        private String statutCompte;
        private Instant dateDeCreation;

        public static UserDto from(User u) {
            UserDto dto = new UserDto();
            dto.setId(u.getId());
            dto.setNom(u.getNom());
            dto.setPrenom(u.getPrenom());
            dto.setEmail(u.getEmail());
            dto.setRole(u.getRole() != null ? u.getRole().name() : null);
            dto.setStatutCompte(u.getStatutCompte());
            dto.setDateDeCreation(u.getDateDeCreation());
            return dto;
        }
    }
}