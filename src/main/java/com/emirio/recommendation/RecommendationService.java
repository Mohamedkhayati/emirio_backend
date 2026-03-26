package com.emirio.recommendation;

import com.emirio.catalog.Article;
import com.emirio.catalog.repo.ArticleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class RecommendationService {

    private final UserArticleEventRepository eventRepository;
    private final RecommendationConfigRepository configRepository;
    private final ArticleRepository articleRepository;

    public RecommendationService(
            UserArticleEventRepository eventRepository,
            RecommendationConfigRepository configRepository,
            ArticleRepository articleRepository
    ) {
        this.eventRepository = eventRepository;
        this.configRepository = configRepository;
        this.articleRepository = articleRepository;
    }

    public void track(Long userId, InteractionRequest req) {
        UserArticleEvent event = new UserArticleEvent(
                userId,
                req.getArticleId(),
                req.getCategoryId(),
                req.getEventType(),
                1
        );
        eventRepository.save(event);
    }

    public RecommendationConfig getConfig() {
        return configRepository.findAll().stream().findFirst().orElseGet(() -> {
            RecommendationConfig cfg = new RecommendationConfig();
            cfg.setStrategy(RecommendationType.HYBRID);
            cfg.setUpdatedAt(LocalDateTime.now());
            return configRepository.save(cfg);
        });
    }

    public RecommendationConfig saveConfig(RecommendationConfigRequest req) {
        RecommendationConfig cfg = getConfig();

        if (req.getStrategy() != null) cfg.setStrategy(req.getStrategy());
        if (req.getFavoriteWeight() != null) cfg.setFavoriteWeight(req.getFavoriteWeight());
        if (req.getClickWeight() != null) cfg.setClickWeight(req.getClickWeight());
        if (req.getOldArticleWeight() != null) cfg.setOldArticleWeight(req.getOldArticleWeight());
        if (req.getBestSellerWeight() != null) cfg.setBestSellerWeight(req.getBestSellerWeight());
        if (req.getOldArticleDays() != null) cfg.setOldArticleDays(req.getOldArticleDays());
        if (req.getLimitCount() != null) cfg.setLimitCount(req.getLimitCount());

        cfg.setUpdatedAt(LocalDateTime.now());
        return configRepository.save(cfg);
    }

    public List<Article> getRecommendations(Long userId) {
        RecommendationConfig cfg = getConfig();
        Map<Long, Double> scores = new HashMap<>();

        if (cfg.getStrategy() == RecommendationType.FAVORITE_CATEGORY || cfg.getStrategy() == RecommendationType.HYBRID) {
            addFavoriteCategoryScores(userId, scores, cfg.getFavoriteWeight());
        }

        if (cfg.getStrategy() == RecommendationType.CLICK_CATEGORY || cfg.getStrategy() == RecommendationType.HYBRID) {
            addClickCategoryScores(userId, scores, cfg.getClickWeight());
        }

        if (cfg.getStrategy() == RecommendationType.OLD_ARTICLES || cfg.getStrategy() == RecommendationType.HYBRID) {
            addOldArticleScores(scores, cfg.getOldArticleWeight());
        }

        if (cfg.getStrategy() == RecommendationType.BEST_SELLERS || cfg.getStrategy() == RecommendationType.HYBRID) {
            addBestSellerScores(scores, cfg.getBestSellerWeight());
        }

        return scores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .map(entry -> articleRepository.findById(entry.getKey()).orElse(null))
                .filter(Objects::nonNull)
                .filter(a -> a.isActif())
                .limit(cfg.getLimitCount())
                .toList();
    }

    private void addFavoriteCategoryScores(Long userId, Map<Long, Double> scores, int weight) {
        List<Object[]> rows = eventRepository.findTopCategories(userId, "FAVORITE");

        for (Object[] row : rows) {
            Long categoryId = ((Number) row[0]).longValue();
            Long count = ((Number) row[1]).longValue();

            List<Article> articles = articleRepository.findActiveByCategorieId(categoryId);
            for (Article article : articles) {
                scores.merge(article.getId(), count * (double) weight, Double::sum);
            }
        }
    }

    private void addClickCategoryScores(Long userId, Map<Long, Double> scores, int weight) {
        List<Object[]> rows = eventRepository.findTopCategories(userId, "VIEW");

        for (Object[] row : rows) {
            Long categoryId = ((Number) row[0]).longValue();
            Long count = ((Number) row[1]).longValue();

            List<Article> articles = articleRepository.findActiveByCategorieId(categoryId);
            for (Article article : articles) {
                scores.merge(article.getId(), count * (double) weight, Double::sum);
            }
        }
    }

    private void addOldArticleScores(Map<Long, Double> scores, int weight) {
        List<Article> oldArticles = articleRepository.findOldArticles();

        for (Article article : oldArticles) {
            scores.merge(article.getId(), (double) weight, Double::sum);
        }
    }

    private void addBestSellerScores(Map<Long, Double> scores, int weight) {
        List<Long> ids = articleRepository.findBestSellerArticleIds();

        int rank = ids.size();
        for (Long id : ids) {
            scores.merge(id, rank * (double) weight, Double::sum);
            rank--;
        }
    }
}