package com.emirio.notification;

import com.emirio.user.User;
import com.emirio.user.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private UserRepository userRepository;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    @Async
    public void sendNewArticleNotification(String articleName, String articleDescription, String articleImageUrl) {
        List<User> activeUsers = userRepository.findByStatutCompteIgnoreCase("ACTIVE");
        if (activeUsers.isEmpty()) {
            log.info("No active users to notify about new article: {}", articleName);
            return;
        }

        String subject = "✨ Nouvel article chez Emirio : " + articleName;

        for (User user : activeUsers) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromEmail);
                helper.setTo(user.getEmail());
                helper.setSubject(subject);
                helper.setText(buildHtmlEmailContent(articleName, articleDescription, articleImageUrl, user.getPrenom()), true);

                mailSender.send(message);
                log.info("New article notification sent to {}", user.getEmail());
            } catch (MessagingException e) {
                log.error("Failed to send email to {} for article {}", user.getEmail(), articleName, e);
            }
        }
    }

    private String buildHtmlEmailContent(String articleName, String description, String imageUrl, String firstName) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<style>" +
                "body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }" +
                ".container { max-width: 600px; margin: 20px auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }" +
                ".header { background-color: #2c3e50; color: white; text-align: center; padding: 20px; }" +
                ".header h1 { margin: 0; font-size: 24px; }" +
                ".content { padding: 20px; }" +
                ".article-image { text-align: center; margin: 20px 0; }" +
                ".article-image img { max-width: 100%; height: auto; border-radius: 8px; }" +
                ".btn { display: inline-block; background-color: #e67e22; color: white; text-decoration: none; padding: 10px 20px; border-radius: 5px; margin-top: 15px; }" +
                ".footer { background-color: #ecf0f1; text-align: center; padding: 15px; font-size: 12px; color: #7f8c8d; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h1>✨ Nouveauté chez Emirio ✨</h1>" +
                "</div>" +
                "<div class='content'>" +
                "<p>Bonjour " + escapeHtml(firstName) + ",</p>" +
                "<p>Nous sommes ravis de vous annoncer l'arrivée d'un nouvel article dans notre catalogue :</p>" +
                "<h2 style='color:#e67e22;'>" + escapeHtml(articleName) + "</h2>" +
                "<div class='article-image'>" +
                (imageUrl != null ? "<img src='" + frontendUrl + imageUrl + "' alt='" + escapeHtml(articleName) + "' />" : "") +
                "</div>" +
                "<p>" + (description != null ? escapeHtml(description) : "") + "</p>" +
                "<a href='" + frontendUrl + "/articles' class='btn'>Découvrir l'article</a>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>© 2025 Emirio - Tous droits réservés<br>" +
                "Vous recevez cet email car vous êtes inscrit sur notre plateforme.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}