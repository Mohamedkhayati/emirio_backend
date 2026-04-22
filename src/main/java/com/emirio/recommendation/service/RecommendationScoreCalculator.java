package com.emirio.recommendation.service;

import com.emirio.catalog.Article;
import com.emirio.catalog.Category;
import com.emirio.catalog.CategoryLevel;
import com.emirio.cart.InteractionPanier;
import com.emirio.cart.TypeActionPanier;
import com.emirio.cart.repo.InteractionPanierRepository;
import com.emirio.favorite.Favorite;
import com.emirio.favorite.FavoriteRepository;
import com.emirio.order.LigneCommande;
import com.emirio.order.repo.LigneCommandeRepository;
import com.emirio.recommendation.entity.ProductView;
import com.emirio.recommendation.repository.ProductViewRepository;
import com.emirio.user.User;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Component
public class RecommendationScoreCalculator {

    private static final double VIEW_WEIGHT = 0.2;
    private static final double FAVORITE_WEIGHT = 2.0;
    private static final double CART_WEIGHT = 4.0;
    private static final double PURCHASE_WEIGHT = 8.0;

    private static final double MAIN_CAT_BONUS = 3.0;
    private static final double SUB_CAT_BONUS = 25.0;
    private static final double LEAF_CAT_BONUS = 4.0;

    private double getRecencyFactor(LocalDateTime date) {
        long days = ChronoUnit.DAYS.between(date, LocalDateTime.now());
        if (days <= 7) return 1.0;
        if (days <= 30) return 0.15;
        return 0.0;
    }

    public double calculateInteractionScore(Article article, User user,
                                            ProductViewRepository viewRepo,
                                            FavoriteRepository favRepo,
                                            InteractionPanierRepository cartRepo,
                                            LigneCommandeRepository purchaseRepo) {
        double score = 0.0;

        // Views
        for (ProductView v : viewRepo.findByUser(user)) {
            if (v.getArticle().getId().equals(article.getId())) {
                score += VIEW_WEIGHT * getRecencyFactor(v.getViewedAt());
            }
        }

        // Favorites
        for (Favorite f : favRepo.findByUser(user)) {
            if (f.getArticle().getId().equals(article.getId())) {
                score += FAVORITE_WEIGHT * getRecencyFactor(f.getCreatedAt());
            }
        }

        // Cart additions
        List<InteractionPanier> cartActions = cartRepo.findByUtilisateurAndTypeAction(user, TypeActionPanier.AJOUT_ARTICLE);
        for (InteractionPanier a : cartActions) {
            if (a.getArticleId() != null && a.getArticleId().equals(article.getId())) {
                score += CART_WEIGHT * getRecencyFactor(a.getDateAction());
            }
        }

        // Purchases
        for (LigneCommande lc : purchaseRepo.findByCommandeClient(user)) {
            if (lc.getArticleId().equals(article.getId())) {
                score += PURCHASE_WEIGHT * getRecencyFactor(lc.getCommande().getDateCommande());
            }
        }
        return score;
    }

    public double calculateCategoryBonus(Article article,
                                         Set<Long> userMainCatIds,
                                         Set<Long> userSubCatIds,
                                         Set<Long> userLeafCatIds) {
        double bonus = 0.0;
        Category cat = article.getCategorie();
        if (cat == null) return 0.0;

        if (cat.getMainCategory() != null && userMainCatIds.contains((long) cat.getMainCategory().ordinal())) {
            bonus += MAIN_CAT_BONUS;
        }
        if (cat.getLevel() == CategoryLevel.SUB && userSubCatIds.contains(cat.getId())) {
            bonus += SUB_CAT_BONUS;
        }
        if (cat.getLevel() == CategoryLevel.SUB_SUB && userLeafCatIds.contains(cat.getId())) {
            bonus += LEAF_CAT_BONUS;
        }
        return bonus;
    }
}