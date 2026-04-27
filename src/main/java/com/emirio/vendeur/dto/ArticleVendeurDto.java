package com.emirio.vendeur.dto;

import com.emirio.catalog.Article;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArticleVendeurDto {
    private Long id;
    private String nom;
    private String description;
    private String details;
    private double prix;
    private boolean actif;
    private Long categorieId;
    private String categorieNom;
    private String marque;
    private String matiere;
    private String sku;
    private String imageUrl;
    private Double salePrice;
    private LocalDateTime saleStartAt;
    private LocalDateTime saleEndAt;
    private boolean recommended;
    private List<VariationVendeurDto> variations;

    public static ArticleVendeurDto from(Article article, List<VariationVendeurDto> variationDtos) {
        ArticleVendeurDto dto = new ArticleVendeurDto();
        dto.setId(article.getId());
        dto.setNom(article.getNom());
        dto.setDescription(article.getDescription());
        dto.setDetails(article.getDetails());
        dto.setPrix(article.getPrix());
        dto.setActif(article.isActif());
        dto.setCategorieId(article.getCategorie() != null ? article.getCategorie().getId() : null);
        dto.setCategorieNom(article.getCategorie() != null ? article.getCategorie().getNom() : null);
        dto.setMarque(article.getMarque());
        dto.setMatiere(article.getMatiere());
        dto.setSku(article.getSku());
        dto.setSalePrice(article.getSalePrice());
        dto.setSaleStartAt(article.getSaleStartAt());
        dto.setSaleEndAt(article.getSaleEndAt());
        dto.setRecommended(article.isRecommended());
        dto.setVariations(variationDtos);
        return dto;
    }
}