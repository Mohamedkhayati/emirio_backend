package com.emirio.auth.passwordreset;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/password")
public class PasswordResetController {

    private final PasswordResetService service;

    public PasswordResetController(PasswordResetService service) {
        this.service = service;
    }

    public record RequestResetDto(@Email @NotBlank String email) {}
    public record ConfirmResetDto(
            @Email @NotBlank String email,
            @NotBlank String code,
            @NotBlank @Size(min = 6, max = 100) String newPassword
    ) {}

    @PostMapping("/reset/request")
    public ResponseEntity<?> request(@RequestBody RequestResetDto dto) {
        service.requestReset(dto.email());
        // Always same message to avoid account enumeration. [web:453]
        return ResponseEntity.ok().body("If the account exists, a code was sent.");
    }

    @PostMapping("/reset/confirm")
    public ResponseEntity<?> confirm(@RequestBody ConfirmResetDto dto) {
        service.confirmReset(dto.email(), dto.code(), dto.newPassword());
        return ResponseEntity.ok().body("Password updated.");
    }
}
