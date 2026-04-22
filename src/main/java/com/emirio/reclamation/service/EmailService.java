package com.emirio.reclamation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendReclamationReply(String to, String reclamationSubject, String replyContent, String reclamationId) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Réponse à votre réclamation #" + reclamationId);

            Context context = new Context();
            context.setVariable("reclamationSubject", reclamationSubject);
            context.setVariable("replyContent", replyContent);
            context.setVariable("reclamationId", reclamationId);
            String html = templateEngine.process("reclamation-reply", context);

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Reply email sent to {} for reclamation #{}", to, reclamationId);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}