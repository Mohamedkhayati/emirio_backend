package com.emirio.admin;

import com.emirio.user.Role;
import com.emirio.user.User;
import com.emirio.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final AdminAuditLogRepository auditLogs;
    private final PasswordEncoder passwordEncoder;
    private final CurrentActorService currentActorService;

    public AdminController(
        UserRepository users,
        AdminAuditLogRepository auditLogs,
        PasswordEncoder passwordEncoder,
        CurrentActorService currentActorService
    ) {
        this.users = users;
        this.auditLogs = auditLogs;
        this.passwordEncoder = passwordEncoder;
        this.currentActorService = currentActorService;
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

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createUser(@Valid @RequestBody CreateUserReq req) {
        currentActorService.requireGeneralAdmin();

        String email = normalizeEmail(req.getEmail());
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        User u = new User();
        u.setNom(req.getNom().trim());
        u.setPrenom(req.getPrenom().trim());
        u.setEmail(email);
        u.setMdp(passwordEncoder.encode(req.getPassword()));
        u.setRole(parseRole(req.getRole()));

        String status = (req.getStatutCompte() == null || req.getStatutCompte().isBlank())
            ? "ACTIVE"
            : normalizeStatus(req.getStatutCompte());
        u.setStatutCompte(status);

        if (u.getDateDeCreation() == null) {
            u.setDateDeCreation(Instant.now());
        }

        users.save(u);
        audit(AdminAction.CREATE, u, "Created account with role=" + u.getRole() + ", status=" + u.getStatutCompte());

        return UserDto.from(u);
    }

    @PutMapping("/users/{id}")
    public UserDto updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserReq req) {
        currentActorService.requireGeneralAdmin();

        User u = requireUser(id);
        protectMainAdmin(u);

        String oldNom = u.getNom();
        String oldPrenom = u.getPrenom();
        String oldEmail = u.getEmail();

        String newEmail = normalizeEmail(req.getEmail());
        if (!newEmail.equalsIgnoreCase(oldEmail) && users.existsByEmailIgnoreCase(newEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        u.setNom(req.getNom().trim());
        u.setPrenom(req.getPrenom().trim());
        u.setEmail(newEmail);

        users.save(u);
        audit(
            AdminAction.EDIT,
            u,
            "Edited user info: nom=" + oldNom + "->" + u.getNom()
                + ", prenom=" + oldPrenom + "->" + u.getPrenom()
                + ", email=" + oldEmail + "->" + u.getEmail()
        );

        return UserDto.from(u);
    }

    @PutMapping({"/users/{id}/status", "/clients/{id}/status"})
    public UserDto updateStatus(@PathVariable Long id, @Valid @RequestBody StatusReq req) {
        currentActorService.requireGeneralAdmin();

        User u = requireUser(id);
        protectMainAdmin(u);

        String oldStatus = u.getStatutCompte();
        u.setStatutCompte(normalizeStatus(req.getStatutCompte()));
        users.save(u);

        audit(AdminAction.UPDATE_STATUS, u, "Status changed: " + oldStatus + " -> " + u.getStatutCompte());
        return UserDto.from(u);
    }

    @PutMapping("/users/{id}/role")
    public UserDto updateRole(@PathVariable Long id, @Valid @RequestBody RoleReq req) {
        currentActorService.requireGeneralAdmin();

        User u = requireUser(id);
        protectMainAdmin(u);

        Role oldRole = u.getRole();
        Role newRole = parseRole(req.getRole());

        u.setRole(newRole);
        users.save(u);

        audit(AdminAction.UPDATE_ROLE, u, "Role changed: " + oldRole + " -> " + newRole);
        return UserDto.from(u);
    }

    @DeleteMapping({"/users/{id}", "/clients/{id}"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        currentActorService.requireGeneralAdmin();

        User u = requireUser(id);
        protectMainAdmin(u);

        audit(AdminAction.DELETE, u, "Deleted account");
        users.deleteById(id);
    }

    @GetMapping("/history")
    public List<AdminAuditLogDto> history() {
        currentActorService.requireGeneralAdmin();
        return auditLogs.findAllByOrderByCreatedAtDesc().stream()
            .map(AdminAuditLogDto::from)
            .toList();
    }

    @GetMapping("/users/{id}/history")
    public List<AdminAuditLogDto> userHistory(@PathVariable Long id) {
        currentActorService.requireGeneralAdmin();
        return auditLogs.findByTargetUserIdOrderByCreatedAtDesc(id).stream()
            .map(AdminAuditLogDto::from)
            .toList();
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

    private void audit(AdminAction action, User target, String details) {
        AdminAuditLog log = new AdminAuditLog();
        log.setAction(action);
        log.setTargetUserId(target.getId());
        log.setTargetEmail(target.getEmail());
        log.setActorEmail(currentActorService.currentActorEmailOrSystem());
        log.setCreatedAt(Instant.now());
        log.setDetails(details);
        auditLogs.save(log);
    }

    private Role parseRole(String rawRole) {
        try {
            return Role.valueOf(rawRole.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role");
        }
    }

    private String normalizeStatus(String status) {
        String value = status == null ? "" : status.trim().toUpperCase();

        if (!value.equals("ACTIVE") && !value.equals("BLOCKED") && !value.equals("DISABLED")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid account status");
        }

        return value;
    }

    private String normalizeEmail(String email) {
        String value = email == null ? "" : email.trim().toLowerCase();
        if (value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        return value;
    }

    @Data
    public static class CreateUserReq {
        @NotBlank
        private String nom;

        @NotBlank
        private String prenom;

        @Email
        @NotBlank
        private String email;

        @NotBlank
        @Size(min = 8, max = 120)
        private String password;

        @NotBlank
        private String role;

        private String statutCompte;
    }

    @Data
    public static class UpdateUserReq {
        @NotBlank
        private String nom;

        @NotBlank
        private String prenom;

        @Email
        @NotBlank
        private String email;
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

    @Data
    public static class AdminAuditLogDto {
        private Long id;
        private String action;
        private Long targetUserId;
        private String targetEmail;
        private String actorEmail;
        private String details;
        private Instant createdAt;

        public static AdminAuditLogDto from(AdminAuditLog log) {
            AdminAuditLogDto dto = new AdminAuditLogDto();
            dto.setId(log.getId());
            dto.setAction(log.getAction() != null ? log.getAction().name() : null);
            dto.setTargetUserId(log.getTargetUserId());
            dto.setTargetEmail(log.getTargetEmail());
            dto.setActorEmail(log.getActorEmail());
            dto.setDetails(log.getDetails());
            dto.setCreatedAt(log.getCreatedAt());
            return dto;
        }
    }
}