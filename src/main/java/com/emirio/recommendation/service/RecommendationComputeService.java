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
        List<User> users = userRepository.findAll();
        for (User user : users) {
            computeForUser(user);
        }
        log.info("Finished recommendation computation");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        log.info("Running initial recommendation computation on startup");
        computeAllRecommendations();
    }

    @Transactional
    public void computeForUser(User user) {
        log.debug("Computing recommendations for user: {}", user.getEmail());

        List<Article> candidates = articleRepository.findByActifTrueOrderByIdDesc();
        if (candidates.isEmpty()) return;

        Set<Long> userMainCatIds = new HashSet<>();
        Set<Long> userSubCatIds = new HashSet<>();
        Set<Long> userLeafCatIds = new HashSet<>();
        collectUserCategories(user, userMainCatIds, userSubCatIds, userLeafCatIds);

        Map<Article, Double> scores = new HashMap<>();
        for (Article article : candidates) {
            double interaction = scoreCalculator.calculateInteractionScore(
                    article, user, productViewRepository, favoriteRepository,
                    cartRepository, purchaseRepository);
            double bonus = scoreCalculator.calculateCategoryBonus(
                    article, userMainCatIds, userSubCatIds, userLeafCatIds);
            double total = interaction + bonus;
            if (total > 0) {
                scores.put(article, total);
            }
        }

        List<Map.Entry<Article, Double>> sorted = scores.entrySet().stream()
                .sorted(Map.Entry.<Article, Double>comparingByValue().reversed())
                .limit(20)
                .toList();

        userRecommendationRepository.deleteByUser(user);

        LocalDateTime now = LocalDateTime.now();
        int rank = 1;
        List<UserRecommendation> newRecs = new ArrayList<>();
        for (Map.Entry<Article, Double> entry : sorted) {
            newRecs.add(new UserRecommendation(user, entry.getKey(), entry.getValue(), rank++, now));
        }
        userRecommendationRepository.saveAll(newRecs);
        log.debug("Saved {} recommendations for user {}", newRecs.size(), user.getEmail());
    }

    private void collectUserCategories(User user,
                                       Set<Long> mainIds,
                                       Set<Long> subIds,
                                       Set<Long> leafIds) {
        // Views
        List<ProductView> views = productViewRepository.findByUser(user);
        for (ProductView view : views) {
            addCategory(view.getArticle().getCategorie(), mainIds, subIds, leafIds);
        }
        // Favorites
        List<Favorite> favorites = favoriteRepository.findByUser(user);
        for (Favorite fav : favorites) {
            addCategory(fav.getArticle().getCategorie(), mainIds, subIds, leafIds);
        }
        // Cart additions
        List<InteractionPanier> cartActions = cartRepository.findByUtilisateurAndTypeAction(user, TypeActionPanier.AJOUT_ARTICLE);
        for (InteractionPanier action : cartActions) {
            if (action.getArticleId() != null) {
                articleRepository.findById(action.getArticleId())
                        .ifPresent(article -> addCategory(article.getCategorie(), mainIds, subIds, leafIds));
            }
        }
        // Purchases
        List<LigneCommande> purchases = purchaseRepository.findByCommandeClient(user);
        for (LigneCommande line : purchases) {
            articleRepository.findById(line.getArticleId())
                    .ifPresent(article -> addCategory(article.getCategorie(), mainIds, subIds, leafIds));
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