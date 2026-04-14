package com.emirio.order.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderSummaryDto {
    private Long id;
    private String referenceCommande;
    private LocalDateTime dateCommande;
    private String statutCommande;
    private double total;
    private int nombreLignes;
    private boolean archived;
}