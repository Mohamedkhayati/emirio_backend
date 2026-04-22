// com.emirio.reclamation.dto.HistoryEntryResponse
package com.emirio.reclamation.dto;

import com.emirio.reclamation.entity.ReclamationHistory;
import lombok.Data;
import java.time.Instant;

@Data
public class HistoryEntryResponse {
    private Long id;
    private String actorName;
    private String actorEmail;
    private String action;
    private String oldValue;
    private String newValue;
    private String details;
    private Instant createdAt;
    private String actorRole;


    public static HistoryEntryResponse from(ReclamationHistory history) {
        HistoryEntryResponse dto = new HistoryEntryResponse();
        dto.setId(history.getId());
        dto.setActorName(history.getActor().getNom() + " " + history.getActor().getPrenom());
        dto.setActorEmail(history.getActor().getEmail());
        dto.setAction(history.getAction());
        dto.setOldValue(history.getOldValue());
        dto.setNewValue(history.getNewValue());
        dto.setDetails(history.getDetails());
        dto.setCreatedAt(history.getCreatedAt());
        dto.setActorRole(history.getActor().getRole().getName());

        return dto;
    }
}