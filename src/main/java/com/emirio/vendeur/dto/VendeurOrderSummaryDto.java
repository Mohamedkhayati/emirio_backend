package com.emirio.vendeur.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VendeurOrderSummaryDto {
    private Long id;
    private String referenceCommande;
    private LocalDateTime dateCommande;
    private String statutCommande;
    private String statutPaiement;
    private double total;
    private String nomClient;
    private String prenomClient;
    private int nombreLignesVendeur; // how many lines belong to this seller
}