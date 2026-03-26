package com.emirio.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CartItemRequest {

    private Long articleId;
    private Long variationId;

    @NotBlank
    private String nom;

    @Positive
    private double prix;

    @Min(1)
    private int qty;

    private String imageUrl;
    private String couleurNom;
    private String taillePointure;
}