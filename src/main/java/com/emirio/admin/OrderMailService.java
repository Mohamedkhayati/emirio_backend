package com.emirio.admin;

import com.emirio.order.Commande;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class OrderMailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:no-reply@emirio.tn}")
    private String from;

    public OrderMailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }

    private void send(String to, String subject, String body) {
        if (to == null || to.isBlank()) return;

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ignored) {
        }
    }

    public void sendInvoiceEmail(Commande c) {
        send(
            c.getEmailClient(),
            "Facture - " + safe(c.getReferenceCommande()),
            "Bonjour " + safe(c.getPrenomClient()) + " " + safe(c.getNomClient()) + ",\n\n" +
            "Votre commande a bien été enregistrée.\n\n" +
            "Référence commande : " + safe(c.getReferenceCommande()) + "\n" +
            "Numéro facture : " + safe(c.getInvoiceNumber()) + "\n" +
            "Mode de paiement : " + (c.getModePaiement() == null ? "" : c.getModePaiement().name()) + "\n" +
            "Instructions de paiement : " + safe(c.getPaymentInstructions()) + "\n" +
            "Lien facture : " + safe(c.getInvoiceUrl()) + "\n\n" +
            "EMIRIO"
        );
    }

    public void sendConfirmedEmail(Commande c) {
        send(
            c.getEmailClient(),
            "Commande confirmée - " + safe(c.getReferenceCommande()),
            "Bonjour " + safe(c.getPrenomClient()) + " " + safe(c.getNomClient()) + ",\n\n" +
            "Votre commande " + safe(c.getReferenceCommande()) + " a été confirmée.\n\nEMIRIO"
        );
    }

    public void sendCancelledEmail(Commande c) {
        send(
            c.getEmailClient(),
            "Commande annulée - " + safe(c.getReferenceCommande()),
            "Bonjour " + safe(c.getPrenomClient()) + " " + safe(c.getNomClient()) + ",\n\n" +
            "Votre commande " + safe(c.getReferenceCommande()) + " a été annulée.\n\nEMIRIO"
        );
    }

    public void sendArchivedEmail(Commande c) {
        send(
            c.getEmailClient(),
            "Commande archivée - " + safe(c.getReferenceCommande()),
            "Bonjour " + safe(c.getPrenomClient()) + " " + safe(c.getNomClient()) + ",\n\n" +
            "Votre commande " + safe(c.getReferenceCommande()) + " a été archivée.\n\nEMIRIO"
        );
    }

    public void sendPaymentAcceptedEmail(Commande c) {
        send(
            c.getEmailClient(),
            "Paiement accepté - " + safe(c.getReferenceCommande()),
            "Bonjour " + safe(c.getPrenomClient()) + " " + safe(c.getNomClient()) + ",\n\n" +
            "Votre paiement a été accepté pour la commande " + safe(c.getReferenceCommande()) + ".\n" +
            "Numéro facture : " + safe(c.getInvoiceNumber()) + "\n" +
            "Lien facture : " + safe(c.getInvoiceUrl()) + "\n\n" +
            "EMIRIO"
        );
    }

    public void sendPaymentRejectedEmail(Commande c) {
        send(
            c.getEmailClient(),
            "Paiement refusé - " + safe(c.getReferenceCommande()),
            "Bonjour " + safe(c.getPrenomClient()) + " " + safe(c.getNomClient()) + ",\n\n" +
            "Votre paiement a été refusé pour la commande " + safe(c.getReferenceCommande()) + ".\n" +
            "Instructions de paiement : " + safe(c.getPaymentInstructions()) + "\n" +
            "Lien facture : " + safe(c.getInvoiceUrl()) + "\n\n" +
            "EMIRIO"
        );
    }

    public void sendDeliveredEmail(Commande c) {
        send(
            c.getEmailClient(),
            "Commande livrée - " + safe(c.getReferenceCommande()),
            "Bonjour " + safe(c.getPrenomClient()) + " " + safe(c.getNomClient()) + ",\n\n" +
            "Votre commande " + safe(c.getReferenceCommande()) + " a été marquée comme livrée.\n\nEMIRIO"
        );
    }
}