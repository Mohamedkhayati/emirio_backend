package com.emirio.reclamation.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class HistoryEntryDto {
    private Long id;
    private String actorName;
    private String actorRole;
    private String action;
    private String oldValue;
    private String newValue;
    private String details;
    private Instant createdAt;
}