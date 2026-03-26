package com.emirio.order.dto;

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
    private String note;
}