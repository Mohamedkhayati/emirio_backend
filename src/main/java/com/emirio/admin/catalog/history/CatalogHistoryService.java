package com.emirio.admin.catalog.history;

import com.emirio.admin.CurrentActorService;
import com.emirio.catalog.Article;
import com.emirio.catalog.VariationArticle;
import com.emirio.user.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CatalogHistoryService {

    private final CatalogHistoryRepository historyRepository;
    private final ObjectMapper objectMapper;
    private final CurrentActorService currentActorService;

    public void articleCreated(Article article) {
        articleCreated(article, currentActorService.requireCurrentUser());
    }

    public void articleUpdated(Article article) {
        articleUpdated(article, currentActorService.requireCurrentUser());
    }

    public void articleDeleted(Article article) {
        articleDeleted(article, currentActorService.requireCurrentUser());
    }

    public void variationCreated(VariationArticle variation) {
        variationCreated(variation, currentActorService.requireCurrentUser());
    }

    public void variationUpdated(VariationArticle variation) {
        variationUpdated(variation, currentActorService.requireCurrentUser());
    }

    public void variationDeleted(VariationArticle variation) {
        variationDeleted(variation, currentActorService.requireCurrentUser());
    }

    public void articleCreated(Article article, User actor) {
        saveArticle(article, actor, CatalogAction.CREATE);
    }

    public void articleUpdated(Article article, User actor) {
        saveArticle(article, actor, CatalogAction.UPDATE);
    }

    public void articleDeleted(Article article, User actor) {
        saveArticle(article, actor, CatalogAction.DELETE);
    }

    public void variationCreated(VariationArticle variation, User actor) {
        saveVariation(variation, actor, CatalogAction.CREATE);
    }

    public void variationUpdated(VariationArticle variation, User actor) {
        saveVariation(variation, actor, CatalogAction.UPDATE);
    }

    public void variationDeleted(VariationArticle variation, User actor) {
        saveVariation(variation, actor, CatalogAction.DELETE);
    }

    private void saveArticle(Article article, User actor, CatalogAction action) {
        CatalogHistory h = new CatalogHistory();
        h.setTargetType(CatalogTargetType.ARTICLE);
        h.setTargetId(article.getId());
        h.setArticleId(article.getId());
        h.setArticleName(article.getNom());
        h.setAction(action);
        fillActor(h, actor);
        h.setSummary(buildArticleSummary(action, article.getNom()));

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("targetType", "ARTICLE");
        details.put("targetId", article.getId());
        details.put("articleId", article.getId());
        details.put("articleName", article.getNom());
        details.put("prix", article.getPrix());
        details.put("actif", article.isActif());
        details.put("categorieId", article.getCategorie() != null ? article.getCategorie().getId() : null);
        details.put("sku", article.getSku());
        details.put("recommended", article.isRecommended());

        h.setDetailsJson(toJson(details));
        historyRepository.save(h);
    }

    private void saveVariation(VariationArticle v, User actor, CatalogAction action) {
        CatalogHistory h = new CatalogHistory();
        h.setTargetType(CatalogTargetType.VARIATION);
        h.setTargetId(v.getId());
        h.setArticleId(v.getArticle() != null ? v.getArticle().getId() : null);
        h.setVariationId(v.getId());
        h.setArticleName(v.getArticle() != null ? v.getArticle().getNom() : null);
        h.setVariationLabel(buildVariationLabel(v));
        h.setAction(action);
        fillActor(h, actor);
        h.setSummary(buildVariationSummary(action, h.getArticleName(), h.getVariationLabel()));

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("targetType", "VARIATION");
        details.put("targetId", v.getId());
        details.put("variationId", v.getId());
        details.put("articleId", v.getArticle() != null ? v.getArticle().getId() : null);
        details.put("articleName", v.getArticle() != null ? v.getArticle().getNom() : null);
        details.put("variationLabel", buildVariationLabel(v));
        details.put("prix", v.getPrix());
        details.put("quantiteStock", v.getQuantiteStock());
        details.put("couleurId", v.getCouleur() != null ? v.getCouleur().getId() : null);
        details.put("tailleId", v.getTaille() != null ? v.getTaille().getId() : null);
        details.put("imagesCount", v.getImages() != null ? v.getImages().size() : 0);
        details.put("hasModel3d", v.getModel3dData() != null && v.getModel3dData().length > 0);

        h.setDetailsJson(toJson(details));
        historyRepository.save(h);
    }

    private void fillActor(CatalogHistory h, User actor) {
        if (actor == null) {
            h.setActorUserId(null);
            h.setActorFullName("SYSTEM");
            h.setActorEmail("SYSTEM");
            return;
        }

        h.setActorUserId(actor.getId());
        h.setActorFullName(buildFullName(actor));
        h.setActorEmail(actor.getEmail());
    }

    private String buildArticleSummary(CatalogAction action, String articleName) {
        String name = articleName == null || articleName.isBlank() ? "(no name)" : articleName.trim();
        return switch (action) {
            case CREATE -> "Article created: " + name;
            case UPDATE -> "Article edited: " + name;
            case DELETE -> "Article deleted: " + name;
        };
    }

    private String buildVariationSummary(CatalogAction action, String articleName, String variationLabel) {
        String article = articleName == null || articleName.isBlank() ? "(no article)" : articleName.trim();
        String variation = variationLabel == null || variationLabel.isBlank() ? "(no variation)" : variationLabel.trim();

        return switch (action) {
            case CREATE -> "Variation created: " + article + " / " + variation;
            case UPDATE -> "Variation edited: " + article + " / " + variation;
            case DELETE -> "Variation deleted: " + article + " / " + variation;
        };
    }

    private String buildVariationLabel(VariationArticle v) {
        String couleur = v.getCouleur() != null && v.getCouleur().getNom() != null
            ? v.getCouleur().getNom()
            : "?";

        String taille = v.getTaille() != null && v.getTaille().getPointure() != null
            ? String.valueOf(v.getTaille().getPointure())
            : "?";

        return couleur + " / " + taille;
    }

    private String buildFullName(User actor) {
        String prenom = actor.getPrenom() == null ? "" : actor.getPrenom().trim();
        String nom = actor.getNom() == null ? "" : actor.getNom().trim();
        String full = (prenom + " " + nom).trim();

        if (!full.isBlank()) {
            return full;
        }
        if (actor.getEmail() != null && !actor.getEmail().isBlank()) {
            return actor.getEmail();
        }
        return "SYSTEM";
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}