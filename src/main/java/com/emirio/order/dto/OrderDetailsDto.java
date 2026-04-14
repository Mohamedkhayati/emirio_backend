package com.emirio.order.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDetailsDto {
    private Long id;
    private String referenceCommande;
    private LocalDateTime dateCommande;
    private String statutCommande;
    private String statutPaiement;
    private double total;
    private boolean archived;
    private boolean cancelable;

    private String nomClient;
    private String prenomClient;
    private String emailClient;
    private String telephone;
    private String adresse;
    private String ville;
    private String codePostal;
    private String modePaiement;
    private String note;

    private String invoiceNumber;
    private String invoiceUrl;
    private String paymentInstructions;

    private List<OrderLineDto> lignes;
}