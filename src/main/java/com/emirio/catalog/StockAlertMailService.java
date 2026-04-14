package com.emirio.catalog;

import com.emirio.user.Role;
import com.emirio.user.User;
import com.emirio.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class StockAlertMailService {

    private static final Logger log = LoggerFactory.getLogger(StockAlertMailService.class);

    private final JavaMailSender mailSender;
    private final UserRepository users;

    @Value("${spring.mail.username:no-reply@emirio.tn}")
    private String from;

    public StockAlertMailService(JavaMailSender mailSender, UserRepository users) {
        this.mailSender = mailSender;
        this.users = users;
    }

    public void sendOutOfStockAlert(VariationArticle variation, int currentStock) {
        Set<String> recipients = new LinkedHashSet<>();

        List<User> admins = users.findByRoleAndStatutCompteIgnoreCase(Role.ADMIN_GENERAL, "ACTIVE");
        for (User admin : admins) {
            if (admin.getEmail() != null && !admin.getEmail().isBlank()) {
                recipients.add(admin.getEmail().trim().toLowerCase());
            }
        }

        if (variation.getArticle() != null && variation.getArticle().getVendeur() != null) {
            User vendeur = variation.getArticle().getVendeur();
            if ("ACTIVE".equalsIgnoreCase(vendeur.getStatutCompte())
                && vendeur.getEmail() != null
                && !vendeur.getEmail().isBlank()) {
                recipients.add(vendeur.getEmail().trim().toLowerCase());
            }
        }

        if (recipients.isEmpty()) {
            return;
        }

        String articleName = variation.getArticle() != null ? safe(variation.getArticle().getNom()) : "Article inconnu";
        String couleur = variation.getCouleur() != null ? safe(variation.getCouleur().getNom()) : "Sans couleur";
        String taille = variation.getTaille() != null
            ? "ID " + variation.getTaille().getId()
            : "Sans taille";

        String subject = "Rupture de stock - " + articleName;

        String body =
            "Bonjour,\n\n" +
            "Une variation est en rupture de stock.\n\n" +
            "Article : " + articleName + "\n" +
            "Variation ID : " + variation.getId() + "\n" +
            "Couleur : " + couleur + "\n" +
            "Taille : " + taille + "\n" +
            "Stock actuel : " + currentStock + "\n\n" +
            "Merci de réapprovisionner cet article.\n\n" +
            "EMIRIO";

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(recipients.toArray(new String[0]));
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Failed to send out-of-stock alert for variation {}", variation.getId(), e);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}