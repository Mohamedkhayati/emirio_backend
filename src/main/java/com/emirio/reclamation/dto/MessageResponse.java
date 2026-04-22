package com.emirio.reclamation.dto;

import com.emirio.reclamation.entity.ReclamationMessage;
import lombok.Data;

import java.time.Instant;

@Data
public class MessageResponse {
    private Long id;
    private String senderEmail;
    private String senderName;
    private String senderRole;
    private String content;
    private Instant timestamp;

    public static MessageResponse from(ReclamationMessage msg) {
        MessageResponse dto = new MessageResponse();
        dto.setId(msg.getId());
        dto.setSenderEmail(msg.getSender().getEmail());
        dto.setSenderName(msg.getSender().getNom() + " " + msg.getSender().getPrenom());
        dto.setSenderRole(msg.getSender().getRole() != null ? msg.getSender().getRole().getName() : "UNKNOWN");
        dto.setContent(msg.getContent());
        dto.setTimestamp(msg.getTimestamp());
        return dto;
    }
}