package com.emirio.recommendation.service;

import com.emirio.catalog.Article;
import com.emirio.catalog.Category;
import com.emirio.catalog.CategoryLevel;
import com.emirio.catalog.repo.ArticleRepository;
import com.emirio.cart.InteractionPanier;
import com.emirio.cart.TypeActionPanier;
import com.emirio.cart.repo.InteractionPanierRepository;
import com.emirio.favorite.Favorite;
import com.emirio.favorite.FavoriteRepository;
import com.emirio.order.LigneCommande;
import com.emirio.order.repo.LigneCommandeRepository;
import com.emirio.recommendation.entity.ProductView;
import com.emirio.recommendation.entity.UserRecommendation;
import com.emirio.recommendation.repository.ProductViewRepository;
import com.emirio.recommendation.repository.UserRecommendationRepository;
import com.emirio.user.User;
import com.emirio.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@EnableScheduling
public class RecommendationComputeService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationComputeService.class);

    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final ProductViewRepository productViewRepository;
    private final FavoriteRepository favoriteRepository;
    private final InteractionPanierRepository cartRepository;
    private final LigneCommandeRepository purchaseRepository;
    private final UserRecommendationRepository userRecommendationRepository;
    private final RecommendationScoreCalculator scoreCalculator;

    public RecommendationComputeService(UserRepository userRepository,
                                        ArticleRepository articleRepository,
                                        ProductViewRepository productViewRepository,
                                        FavoriteRepository favoriteRepository,
                                        InteractionPanierRepository cartRepository,
                                        LigneCommandeRepository purchaseRepository,
                                        UserRecommendationRepository userRecommendationRepository,
                                        RecommendationScoreCalculator scoreCalculator) {
        this.userRepository = userRepository;
        this.articleRepository = articleRepository;
        this.productViewRepository = productViewRepository;
        this.favoriteRepository = favoriteRepository;
        this.cartRepository = cartRepository;
        this.purchaseRepository = purchaseRepository;
        this.userRecommendationRepository = userRecommendationRepository;
        this.scoreCalculator = scoreCalculator;
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void computeAllRecommendations() {
        log.info("Starting recommendation computation for all users");
        try {
            List<User> users = userRepository.findAll();
            for (User user : users) {
                try {
                    computeForUser(user);
                } catch (Exception e) {
                    log.error("Failed to compute recommendations for user {}: {}", user.getEmail(), e.getMessage());
                }
            }
            log.info("Finished recommendation computation");
        } catch (Exception e) {
            log.error("Recommendation computation failed: {}", e.getMessage());
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(1000) // Run last after all data is loaded
    public void init() {
        log.info("Running initial recommendation computation on startup");
        // Delay execution to ensure all data is loaded
        new Thread(() -> {
            try {
                Thread.sleep(5000); // Wait 5 seconds before computing
                computeAllRecommendations();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Recommendation initialization interrupted");
            }
        }).start();
    }

    @Transactional
    public void computeForUser(User user) {
        log.debug("Computing recommendations for user: {}", user.getEmail());
        
        try {
            List<Article> candidates = articleRepository.findByActifTrueOrderByIdDesc();
            if (candidates.isEmpty()) {
                log.debug("No active articles found for user: {}", user.getEmail());
                return;
            }

            Set<Long> userMainCatIds = new HashSet<>();
            Set<Long> userSubCatIds = new HashSet<>();
            Set<Long> userLeafCatIds = new HashSet<>();
            collectUserCategories(user, userMainCatIds, userSubCatIds, userLeafCatIds);

            Map<Article, Double> scores = new HashMap<>();
            for (Article article : candidates) {
                try {
                    double interaction = scoreCalculator.calculateInteractionScore(
                            article, user, productViewRepository, favoriteRepository,
                            cartRepository, purchaseRepository);
                    double bonus = scoreCalculator.calculateCategoryBonus(
                            article, userMainCatIds, userSubCatIds, userLeafCatIds);
                    double total = interaction + bonus;
                    if (total > 0) {
                        scores.put(article, total);
                    }
                } catch (Exception e) {
                    log.debug("Error scoring article {} for user {}: {}", article.getId(), user.getEmail(), e.getMessage());
                }
            }

            List<Map.Entry<Article, Double>> sorted = scores.entrySet().stream()
                    .sorted(Map.Entry.<Article, Double>comparingByValue().reversed())
                    .limit(20)
                    .toList();

            // Clear old recommendations
            try {
                userRecommendationRepository.deleteByUser(user);
            } catch (Exception e) {
                log.warn("Could not delete old recommendations for user {}: {}", user.getEmail(), e.getMessage());
            }

            LocalDateTime now = LocalDateTime.now();
            int rank = 1;
            List<UserRecommendation> newRecs = new ArrayList<>();
            for (Map.Entry<Article, Double> entry : sorted) {
                // Verify article still exists before saving
                if (articleRepository.existsById(entry.getKey().getId())) {
                    newRecs.add(new UserRecommendation(user, entry.getKey(), entry.getValue(), rank++, now));
                }
            }
            
            if (!newRecs.isEmpty()) {
                try {
                    userRecommendationRepository.saveAll(newRecs);
                    log.debug("Saved {} recommendations for user {}", newRecs.size(), user.getEmail());
                } catch (DataIntegrityViolationException e) {
                    log.error("Data integrity error saving recommendations for user {}: {}", user.getEmail(), e.getMessage());
                    // Save one by one to find problematic ones
                    for (UserRecommendation rec : newRecs) {
                        try {
                            userRecommendationRepository.save(rec);
                        } catch (DataIntegrityViolationException ex) {
                            log.warn("Could not save recommendation for article {}: {}", rec.getArticle().getId(), ex.getMessage());
                        }
                    }
                }
            } else {
                log.debug("No recommendations generated for user {}", user.getEmail());
            }
        } catch (Exception e) {
            log.error("Failed to compute recommendations for user {}: {}", user.getEmail(), e.getMessage(), e);
            throw e;
        }
    }

    private void collectUserCategories(User user,
                                       Set<Long> mainIds,
                                       Set<Long> subIds,
                                       Set<Long> leafIds) {
        try {
            // Views
            List<ProductView> views = productViewRepository.findByUser(user);
            for (ProductView view : views) {
                if (view.getArticle() != null && view.getArticle().getCategorie() != null) {
                    addCategory(view.getArticle().getCategorie(), mainIds, subIds, leafIds);
                }
            }
        } catch (Exception e) {
            log.debug("Error collecting categories from views: {}", e.getMessage());
        }
        
        try {
            // Favorites
            List<Favorite> favorites = favoriteRepository.findByUser(user);
            for (Favorite fav : favorites) {
                if (fav.getArticle() != null && fav.getArticle().getCategorie() != null) {
                    addCategory(fav.getArticle().getCategorie(), mainIds, subIds, leafIds);
                }
            }
        } catch (Exception e) {
            log.debug("Error collecting categories from favorites: {}", e.getMessage());
        }
        
        try {
            // Cart additions
            List<InteractionPanier> cartActions = cartRepository.findByUtilisateurAndTypeAction(user, TypeActionPanier.AJOUT_ARTICLE);
            for (InteractionPanier action : cartActions) {
                if (action.getArticleId() != null) {
                    articleRepository.findById(action.getArticleId())
                            .filter(article -> article.getCategorie() != null)
                            .ifPresent(article -> addCategory(article.getCategorie(), mainIds, subIds, leafIds));
                }
            }
        } catch (Exception e) {
            log.debug("Error collecting categories from cart: {}", e.getMessage());
        }
        
        try {
            // Purchases
            List<LigneCommande> purchases = purchaseRepository.findByCommandeClient(user);
            for (LigneCommande line : purchases) {
                articleRepository.findById(line.getArticleId())
                        .filter(article -> article.getCategorie() != null)
                        .ifPresent(article -> addCategory(article.getCategorie(), mainIds, subIds, leafIds));
            }
        } catch (Exception e) {
            log.debug("Error collecting categories from purchases: {}", e.getMessage());
        }
    }

    private void addCategory(Category cat, Set<Long> mainIds, Set<Long> subIds, Set<Long> leafIds) {
        if (cat == null) return;
        if (cat.getMainCategory() != null) {
            mainIds.add((long) cat.getMainCategory().ordinal());
        }
        if (cat.getLevel() == CategoryLevel.SUB) {
            subIds.add(cat.getId());
        } else if (cat.getLevel() == CategoryLevel.SUB_SUB) {
            leafIds.add(cat.getId());
            if (cat.getParent() != null) {
                subIds.add(cat.getParent().getId());
            }
        }
    }
}