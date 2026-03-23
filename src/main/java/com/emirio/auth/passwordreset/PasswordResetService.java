package com.emirio.auth.passwordreset;

import com.emirio.user.User;
import com.emirio.user.UserRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

@Service
public class PasswordResetService {

    private final InMemoryPasswordResetStore store;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(
            InMemoryPasswordResetStore store,
            UserRepository userRepo,
            PasswordEncoder passwordEncoder,
            JavaMailSender mailSender
    ) {
        this.store = store;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
    }

    public void requestReset(String emailRaw) {
        String email = normalizeEmail(emailRaw);

        String code = generate6Digits();
        String hash = passwordEncoder.encode(code);

        store.putLatest(email, hash, Instant.now().plus(Duration.ofMinutes(10)));

        userRepo.findByEmail(email).ifPresent(u -> sendCode(u.getEmail(), code));
    }

    public void confirmReset(String emailRaw, String code, String newPassword) {
        String email = normalizeEmail(emailRaw);

        var rec = store.getLatest(email);
        if (rec == null) throw new IllegalArgumentException("Invalid code");
        if (rec.used) throw new IllegalArgumentException("Invalid code");
        if (Instant.now().isAfter(rec.expiresAt)) throw new IllegalArgumentException("Code expired");
        if (rec.attempts >= 5) throw new IllegalArgumentException("Too many attempts");

        rec.attempts++;

        if (!passwordEncoder.matches(code, rec.codeHash)) {
            throw new IllegalArgumentException("Invalid code");
        }

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid code"));

        user.setMdp(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        rec.used = true;
        store.remove(email);
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
