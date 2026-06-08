package com.emirio.auth;

import com.emirio.security.JwtService;
import com.emirio.user.RoleRepository;
import com.emirio.user.User;
import com.emirio.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwt;

    public AuthController(UserRepository users,
                          RoleRepository roles,
                          PasswordEncoder encoder,
                          AuthenticationManager authManager,
                          JwtService jwt) {
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
        this.authManager = authManager;
        this.jwt = jwt;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest req) {
        String email = req.getEmail().trim().toLowerCase();

        if (users.existsByEmailIgnoreCase(email)) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Email already used", "error", "User already exists"));
        }

        var userRole = roles.findByName("Client")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Default role USER not found"));

        User u = User.builder()
                .nom(req.getNom().trim())
                .prenom(req.getPrenom().trim())
                .email(email)
                .mdp(encoder.encode(req.getPassword()))
                .role(userRole)
                .dateDeCreation(Instant.now())
                .statutCompte("ACTIVE")
                .build();

        users.save(u);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of("message", "User created successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        
        // FIRST: Check if user exists
        User user = users.findByEmailIgnoreCase(email)
                .orElse(null);
        
        if (user == null) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                    "message", "User not found. Please sign up first.",
                    "error", "User does not exist"
                ));
        }
        
        // SECOND: Check if account is active
        if (!"ACTIVE".equalsIgnoreCase(user.getStatutCompte())) {
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                    "message", "Account is not active. Please contact support.",
                    "error", "Account inactive"
                ));
        }
        
        // THIRD: Try to authenticate with password
        try {
            authManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, req.getPassword())
            );
        } catch (BadCredentialsException e) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                    "message", "Incorrect password. Please try again.",
                    "error", "Bad credentials"
                ));
        }
        
        // FOURTH: Generate token
        String token = jwt.generateToken(user.getEmail());
        return ResponseEntity.ok(Map.of(
            "token", token,
            "role", user.getRole().getName()
        ));
    }
}