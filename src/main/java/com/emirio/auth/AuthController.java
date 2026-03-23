package com.emirio.auth;

import com.emirio.security.JwtService;
import com.emirio.user.*;
import jakarta.validation.Valid;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwt;

    public AuthController(UserRepository users, PasswordEncoder encoder,
                          AuthenticationManager authManager, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.authManager = authManager;
        this.jwt = jwt;
    }

    @PostMapping("/signup")
    public void signup(@Valid @RequestBody SignupRequest req) {
      if (users.existsByEmail(req.getEmail())) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already used");
      }

      var u = User.builder()
        .nom(req.getNom())
        .prenom(req.getPrenom())
        .email(req.getEmail())
        .mdp(encoder.encode(req.getPassword()))
        .role(Role.CLIENT)
        .dateDeCreation(Instant.now())
        .statutCompte("ACTIVE")
        .build();

      users.save(u);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );
        String token = jwt.generateToken(req.getEmail());
        return new AuthResponse(token);
    }
}
