package com.emirio.cart.dto;

import com.emirio.cart.LignePanier;
import com.emirio.cart.Panier;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PanierResponse {

    private Long id;
    private LocalDateTime dateCreation;
    private LocalDateTime dateMaj;
    private int totalItems;
    private double totalAmount;
    private List<PanierItemResponse> items;

    @Data
    public static class PanierItemResponse {
        private Long id;
        private Long articleId;
        private Long variationId;
        private String nomProduit;
        private String imageUrl;
        private String couleurNom;
        private String taillePointure;
        private int quantite;
        private double prixUnitaire;
        private double sousTotal;

        public static PanierItemResponse from(LignePanier l) {
            PanierItemResponse dto = new PanierItemResponse();
            dto.setId(l.getId());
            dto.setArticleId(l.getArticleId());
            dto.setVariationId(l.getVariationId());
            dto.setNomProduit(l.getNomProduit());
            dto.setImageUrl(l.getImageUrl());
            dto.setCouleurNom(l.getCouleurNom());
            dto.setTaillePointure(l.getTaillePointure());
            dto.setQuantite(l.getQuantite());
            dto.setPrixUnitaire(l.getPrixUnitaire());
            dto.setSousTotal(l.getSousTotal());
            return dto;
        }
    }

    public static PanierResponse from(Panier panier) {
        PanierResponse dto = new PanierResponse();
        dto.setId(panier.getId());
        dto.setDateCreation(panier.getDateCreation());
        dto.setDateMaj(panier.getDateMaj());

        List<PanierItemResponse> lines = panier.getLignes().stream()
            .map(PanierItemResponse::from)
            .toList();

        dto.setItems(lines);
        dto.setTotalItems(lines.stream().mapToInt(PanierItemResponse::getQuantite).sum());
        dto.setTotalAmount(lines.stream().mapToDouble(PanierItemResponse::getSousTotal).sum());
        return dto;
    }
}