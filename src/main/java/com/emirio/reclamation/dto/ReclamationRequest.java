package com.emirio.reclamation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReclamationRequest {
    @NotBlank(message = "Subject is required")
    private String subject;
    
    @NotBlank(message = "Description is required")
    private String description;
}