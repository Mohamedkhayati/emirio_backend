package com.emirio.vendeur.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class VendeurOrderDetailsDto {
    private Long id;
    private String referenceCommande;
    private LocalDateTime dateCommande;
    private String statutCommande;
    private String statutPaiement;
    private double total;
    private String nomClient;
    private String prenomClient;
    private String emailClient;
    private String telephone;
    private String adresse;
    private String ville;
    private String codePostal;
    private String modePaiement;
    private String note;
    private List<VendeurOrderLineDto> lignesVendeur; // only seller's lines
}