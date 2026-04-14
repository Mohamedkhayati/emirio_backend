package com.emirio.cart.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartItemRequest {

    @NotNull
    private Long articleId;

    @NotNull
    private Long variationId;

    @NotBlank
    private String nomProduit;

    @Min(1)
    private int quantite;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private Double prixUnitaire;

    private String imageUrl;
    private String couleurNom;
    private String taillePointure;
}