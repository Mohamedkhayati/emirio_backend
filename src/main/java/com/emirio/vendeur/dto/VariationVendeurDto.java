package com.emirio.vendeur.dto;

import com.emirio.catalog.VariationArticle;
import lombok.Data;
import java.util.List;

@Data
public class VariationVendeurDto {
    private Long id;
    private Long articleId;
    private Long couleurId;
    private String couleurNom;
    private String couleurCodeHex;
    private Long tailleId;
    private String taillePointure;
    private Double prix;
    private Integer quantiteStock;
    private String model3dUrl;
    private List<String> imageUrls;

    public static VariationVendeurDto from(VariationArticle v) {
        VariationVendeurDto dto = new VariationVendeurDto();
        dto.setId(v.getId());
        dto.setArticleId(v.getArticle().getId());
        if (v.getCouleur() != null) {
            dto.setCouleurId(v.getCouleur().getId());
            dto.setCouleurNom(v.getCouleur().getNom());
            dto.setCouleurCodeHex(v.getCouleur().getCodeHex());
        }
        if (v.getTaille() != null) {
            dto.setTailleId(v.getTaille().getId());
            dto.setTaillePointure(v.getTaille().getPointure());
        }
        dto.setPrix(v.getPrix());
        dto.setQuantiteStock(v.getQuantiteStock());
        if (v.getModel3dData() != null && v.getModel3dData().length > 0) {
            dto.setModel3dUrl("/api/catalog/variations/" + v.getId() + "/model");
        }
        List<String> urls = v.getImages().stream()
                .filter(img -> img.getId() != null)
                .map(img -> "/api/catalog/variations/" + v.getId() + "/images/" + img.getId())
                .toList();
        dto.setImageUrls(urls);
        return dto;
    }
}