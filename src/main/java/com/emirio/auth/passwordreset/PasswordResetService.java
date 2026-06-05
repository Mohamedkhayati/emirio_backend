package com.emirio.auth.passwordreset;

import com.emirio.user.User;
import com.emirio.user.UserRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

@Service
public class PasswordResetService {

    private final PasswordResetCodeRepository codeRepository;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(
            PasswordResetCodeRepository codeRepository,
            UserRepository userRepo,
            PasswordEncoder passwordEncoder,
            JavaMailSender mailSender
    ) {
        this.codeRepository = codeRepository;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
    }

    @Transactional
    public void requestReset(String emailRaw) {
        String email = normalizeEmail(emailRaw);

        // Delete any existing (unused or expired) codes for this email – mimics overwriting the in-memory entry
        codeRepository.deleteByEmail(email);

        String code = generate6Digits();
        String hash = passwordEncoder.encode(code);

        PasswordResetCode resetCode = new PasswordResetCode();
        resetCode.setEmail(email);
        resetCode.setCodeHash(hash);
        resetCode.setExpiresAt(Instant.now().plus(Duration.ofMinutes(10)));
        resetCode.setUsed(false);
        resetCode.setAttempts(0);
        // createdAt is auto-set in entity

        codeRepository.save(resetCode);

        // Only send email if the user actually exists (prevents enumeration – same message is returned anyway)
        userRepo.findByEmail(email).ifPresent(u -> sendCode(u.getEmail(), code));
    }

    @Transactional
    public void confirmReset(String emailRaw, String code, String newPassword) {
        String email = normalizeEmail(emailRaw);

        PasswordResetCode resetCode = codeRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid code"));

        if (resetCode.isUsed()) {
            throw new IllegalArgumentException("Invalid code");
        }
        if (Instant.now().isAfter(resetCode.getExpiresAt())) {
            throw new IllegalArgumentException("Code expired");
        }
        if (resetCode.getAttempts() >= 5) {
            throw new IllegalArgumentException("Too many attempts");
        }

        // Increment attempt counter and persist immediately
        resetCode.setAttempts(resetCode.getAttempts() + 1);
        codeRepository.save(resetCode);

        if (!passwordEncoder.matches(code, resetCode.getCodeHash())) {
            throw new IllegalArgumentException("Invalid code");
        }

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid code"));

        user.setMdp(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        // Mark code as used (or you could delete all codes for this user)
        resetCode.setUsed(true);
        codeRepository.save(resetCode);

        // Optional: physically remove used/expired codes to keep table clean
        // codeRepository.delete(resetCode);
    }

    private String generate6Digits() {
        return String.valueOf(100000 + random.nextInt(900000));
    }

    private void sendCode(String email, String code) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(email);
        msg.setSubject("Emirio - Password reset code");
        msg.setText("Your verification code is: " + code + "\nIt expires in 10 minutes.");
        mailSender.send(msg);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.toLowerCase().trim();
    }
}