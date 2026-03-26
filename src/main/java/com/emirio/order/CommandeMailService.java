package com.emirio.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class CommandeMailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@emirio.com}")
    private String from;

    public CommandeMailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendCommandePassee(Commande commande) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(commande.getEmailClient());
        message.setSubject("Commande " + commande.getReferenceCommande() + " reçue");
        message.setText(
            "Bonjour " + commande.getPrenomClient() + " " + commande.getNomClient() + ",\n\n" +
            "Votre commande " + commande.getReferenceCommande() + " a bien été enregistrée.\n" +
            "Statut actuel : " + commande.getStatutCommande() + "\n" +
            "Total : " + commande.getTotal() + " TND\n\n" +
            "Merci. Veuillez attendre notre confirmation.\n\n" +
            "EMIRIO"
        );
        mailSender.send(message);
    }
}