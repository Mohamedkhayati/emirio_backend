package com.emirio.admin;

import com.emirio.order.Commande;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class OrderMailService {

    private static final Logger log = LoggerFactory.getLogger(OrderMailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:no-reply@emirio.tn}")
    private String from;

    public OrderMailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    private void send(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            log.warn("Email not sent: recipient is empty");
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }

    public void sendInvoiceEmail(Commande c) {
        send(c.getEmailClient(),
            "Facture - " + safe(c.getReferenceCommande()),
            buildBasicMessage(c) + "\nLien facture : " + safe(c.getInvoiceUrl()));
    }

    public void sendConfirmedEmail(Commande c) {
        send(c.getEmailClient(),
            "Commande confirmée - " + safe(c.getReferenceCommande()),
            buildBasicMessage(c) + "Votre commande a été confirmée.");
    }

    public void sendCancelledEmail(Commande c) {
        send(c.getEmailClient(),
            "Commande annulée - " + safe(c.getReferenceCommande()),
            buildBasicMessage(c) + "Votre commande a été annulée.");
    }

    public void sendArchivedEmail(Commande c) {
        send(c.getEmailClient(),
            "Commande archivée - " + safe(c.getReferenceCommande()),
            buildBasicMessage(c) + "Votre commande a été archivée.");
    }

    public void sendPaymentAcceptedEmail(Commande c) {
        send(c.getEmailClient(),
            "Paiement accepté - " + safe(c.getReferenceCommande()),
            buildBasicMessage(c) + "Votre paiement a été accepté.\nNuméro facture : " + safe(c.getInvoiceNumber()));
    }

    public void sendPaymentRejectedEmail(Commande c) {
        send(c.getEmailClient(),
            "Paiement refusé - " + safe(c.getReferenceCommande()),
            buildBasicMessage(c) + "Votre paiement a été refusé.\nInstructions : " + safe(c.getPaymentInstructions()));
    }

    public void sendDeliveredEmail(Commande c) {
        send(c.getEmailClient(),
            "Commande livrée - " + safe(c.getReferenceCommande()),
            buildBasicMessage(c) + "Votre commande a été marquée comme livrée.");
    }

    private String buildBasicMessage(Commande c) {
        return "Bonjour " + safe(c.getPrenomClient()) + " " + safe(c.getNomClient()) + ",\n\n";
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }
}