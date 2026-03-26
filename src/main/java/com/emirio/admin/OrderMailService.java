package com.emirio.admin;

import com.emirio.order.Commande;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderMailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Async
    public void sendConfirmedEmail(Commande c) {
        send(
            c,
            "Order confirmed - " + ref(c),
            """
            Hello %s,

            Your order %s has been confirmed successfully.
            Current status: %s
            Total: %s TND

            Thank you for shopping with EMIRIO.
            """
                .formatted(fullName(c), ref(c), c.getStatutCommande(), c.getTotal())
        );
    }

    @Async
    public void sendCancelledEmail(Commande c) {
        send(
            c,
            "Order cancelled - " + ref(c),
            """
            Hello %s,

            Your order %s has been cancelled.
            If you need help, please contact our support team.

            Thank you,
            EMIRIO
            """
                .formatted(fullName(c), ref(c))
        );
    }

    @Async
    public void sendArchivedEmail(Commande c) {
        send(
            c,
            "Order archived - " + ref(c),
            """
            Hello %s,

            Your order %s has been archived in our system.
            This is an internal organization action and your order history remains محفوظ.

            Regards,
            EMIRIO
            """
                .formatted(fullName(c), ref(c))
        );
    }

    private void send(Commande c, String subject, String text) {
        if (c == null || c.getEmailClient() == null || c.getEmailClient().isBlank()) {
            log.warn("Mail skipped: missing customer email for order {}", c != null ? c.getId() : null);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(c.getEmailClient());
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);

            log.info("Mail sent to {} for order {}", c.getEmailClient(), c.getId());
        } catch (Exception e) {
            log.error("Failed to send mail to {} for order {}", c.getEmailClient(), c.getId(), e);
        }
    }

    private String safe(String v) {
        return v == null ? "" : v.trim();
    }

    private String fullName(Commande c) {
        String full = (safe(c.getPrenomClient()) + " " + safe(c.getNomClient())).trim();
        return full.isBlank() ? "customer" : full;
    }

    private String ref(Commande c) {
        return c.getReferenceCommande() != null && !c.getReferenceCommande().isBlank()
            ? c.getReferenceCommande()
            : "#" + c.getId();
    }
}