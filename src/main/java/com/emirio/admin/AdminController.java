package com.emirio.admin;

import com.emirio.user.Role;
import com.emirio.user.RoleRepository;
import com.emirio.user.User;
import com.emirio.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AdminController {

    private static final String MAIN_ADMIN_EMAIL = "admin.general@emirio.tn";

    private final RoleRepository roles;
    private final UserRepository users;
    private final AdminAuditLogRepository auditLogs;
    private final PasswordEncoder passwordEncoder;
    private final CurrentActorService currentActorService;

    public AdminController(
            UserRepository users,
            RoleRepository roles,
            AdminAuditLogRepository auditLogs,
            PasswordEncoder passwordEncoder,
            CurrentActorService currentActorService
    ) {
        this.users = users;
        this.roles = roles;
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

    @GetMapping("/check-role")
    public Map<String, Object> checkRole(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", authentication != null);
        if (authentication != null) {
            response.put("name", authentication.getName());
            response.put("authorities", authentication.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .collect(Collectors.toList()));
        }
        return response;
    }

    @GetMapping("/debug/auth")
    public Map<String, Object> debugAuth(Authentication authentication, HttpServletRequest request) {
        Map<String, Object> debug = new HashMap<>();

        debug.put("requestURI", request.getRequestURI());
        debug.put("method", request.getMethod());
        debug.put("hasAuthentication", authentication != null);

        if (authentication != null) {
            debug.put("name", authentication.getName());
            debug.put("authenticated", authentication.isAuthenticated());
            debug.put("authorities", authentication.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .collect(Collectors.toList()));
            debug.put("principal", authentication.getPrincipal().toString());
            debug.put("credentials", authentication.getCredentials() != null ? "present" : "null");
            debug.put("details", authentication.getDetails() != null ? authentication.getDetails().toString() : "null");
        }

        String authHeader = request.getHeader("Authorization");
        debug.put("authorizationHeader", authHeader != null ? "Bearer [present] length=" + authHeader.length() : "null");

        if (authentication != null && authentication.getName() != null) {
            try {
                var user = users.findByEmailIgnoreCase(authentication.getName());
                if (user.isPresent()) {
                    debug.put("dbRole", user.get().getRole() != null ? user.get().getRole().getName() : null);
                    debug.put("dbStatus", user.get().getStatutCompte());
                    debug.put("dbEmail", user.get().getEmail());
                }
            } catch (Exception e) {
                debug.put("dbError", e.getMessage());
            }
        }

        return debug;
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
        audit(AdminAction.CREATE, u, "Created account with role=" + roleName(u) + ", status=" + u.getStatutCompte());

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

        String oldRole = roleName(u);
        Role newRole = parseRole(req.getRole());

        u.setRole(newRole);
        users.save(u);

        audit(AdminAction.UPDATE_ROLE, u, "Role changed: " + oldRole + " -> " + roleName(u));
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
        if (rawRole == null || rawRole.trim().isEmpty()) {
            return roles.findByName("USER")
                    .orElseGet(() -> roles.findByName("Client")
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Default role not found")));
        }
        
        String normalized = rawRole.trim().toUpperCase();
        String targetRoleName;
        
        // Map everything to the EXACT strings stored in your Database & SecurityConfig
        switch (normalized) {
            case "ADMINISTRATEUR":
            case "ADMIN_GENERAL":
            case "ADMIN":
            case "ROLE_ADMIN_GENERAL":
                targetRoleName = "Administrateur";
                break;
                
            case "GESTIONNAIRE DE CATALOGUE":
            case "GESTIONNAIRE_DE_CATALOGUE":
            case "VENDEUR":
            case "ROLE_VENDEUR":
                targetRoleName = "Gestionnaire de catalogue";
                break;
                
            case "RESPONSABLE E-COMMERCE":
            case "RESPONSABLE_E_COMMERCE":
            case "CONTROLEUR":
            case "ROLE_CONTROLEUR":
                targetRoleName = "Responsable e-commerce";
                break;
                
            case "CLIENT":
            case "USER": 
            case "ROLE_USER":
            case "ROLE_CLIENT":
                targetRoleName = "USER"; 
                break;
                
            default:
                targetRoleName = rawRole;
        }

        // 1. Try mapped name -> 2. Try raw string -> 3. Fallback to "Client" if "USER" misses
        return roles.findByName(targetRoleName)
                .orElseGet(() -> roles.findByName(rawRole)
                .orElseGet(() -> roles.findByName("Client") 
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + rawRole))));
    }
    
    private String roleName(User user) {
        return user.getRole() != null ? user.getRole().getName() : null;
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
            dto.setRole(u.getRole() != null ? u.getRole().getName() : null);
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