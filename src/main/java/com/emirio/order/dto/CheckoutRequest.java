package com.emirio.order.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CheckoutRequest {

    private String nom;
    private String prenom;

    @NotBlank
    private String telephone;

    @NotBlank
    private String adresse;

    @NotBlank
    private String ville;

    private String codePostal;

    @NotBlank
    private String modePaiement;

    private String cardLast4;
    private String d17Phone;
    private String d17Reference;
    private String bankReference;

    private String note;
    private String signatureDataUrl;

    @AssertTrue(message = "You must accept terms")
    private boolean acceptTerms;
}