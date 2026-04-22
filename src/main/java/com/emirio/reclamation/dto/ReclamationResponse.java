// com.emirio.reclamation.dto.ReclamationResponse
package com.emirio.reclamation.dto;

import com.emirio.reclamation.entity.Reclamation;
import com.emirio.reclamation.entity.ReclamationHistory;
import lombok.Data;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class ReclamationResponse {
    private Long id;
    private String userEmail;
    private String userName;
    private String subject;
    private String description;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
    private Long userId;
    private List<MessageResponse> messages;
    private List<HistoryEntryResponse> history;  // new field

    public static ReclamationResponse from(Reclamation reclamation) {
        ReclamationResponse dto = new ReclamationResponse();
        dto.setId(reclamation.getId());
        dto.setUserEmail(reclamation.getUser().getEmail());
        dto.setUserName(reclamation.getUser().getNom() + " " + reclamation.getUser().getPrenom());
        dto.setSubject(reclamation.getSubject());
        dto.setDescription(reclamation.getDescription());
        dto.setStatus(reclamation.getStatus());
        dto.setCreatedAt(reclamation.getCreatedAt());
        dto.setUpdatedAt(reclamation.getUpdatedAt());
        dto.setUserId(reclamation.getUser().getId());
        if (reclamation.getMessages() != null) {
            dto.setMessages(reclamation.getMessages().stream()
                    .map(MessageResponse::from)
                    .collect(Collectors.toList()));
        }
        // history will be set separately via another method or join fetch
        return dto;
    }

    public static ReclamationResponse withHistory(Reclamation reclamation, List<ReclamationHistory> historyList) {
        ReclamationResponse dto = from(reclamation);
        if (historyList != null) {
            dto.setHistory(historyList.stream()
                    .map(HistoryEntryResponse::from)
                    .collect(Collectors.toList()));
        }
        return dto;
    }
}