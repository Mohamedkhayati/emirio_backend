package com.emirio.order.dto;

import lombok.Data;

@Data
public class CheckoutRequest {
    private String nom;
    private String prenom;
    private String telephone;
    private String adresse;
    private String ville;
    private String codePostal;

    private String modePaiement;
    private String cardLast4;
    private String d17Phone;
    private String d17Reference;
    private String bankReference;

    private String note;
    private String signatureDataUrl;
    private boolean acceptTerms;
}