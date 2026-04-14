package com.emirio.order.dto;

import lombok.Data;

@Data
public class OrderLineDto {
    private Long id;
    private Long variationId;
    private Long articleId;
    private String articleNom;
    private String couleurNom;
    private String taillePointure;
    private int quantite;
    private double prixUnitaire;
    private double sousTotal;
    private String imageUrl;
}