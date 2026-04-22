package com.emirio.recommendation.controller;

import com.emirio.catalog.Article;
import com.emirio.catalog.repo.ArticleRepository;
import com.emirio.recommendation.entity.ProductView;
import com.emirio.recommendation.repository.ProductViewRepository;
import com.emirio.user.User;
import com.emirio.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/product-views")
public class ProductViewController {

    private static final Logger log = LoggerFactory.getLogger(ProductViewController.class);

    private final ProductViewRepository productViewRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    public ProductViewController(ProductViewRepository productViewRepository,
                                 ArticleRepository articleRepository,
                                 UserRepository userRepository) {
        this.productViewRepository = productViewRepository;
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<String> trackView(@RequestBody Map<String, Long> payload,
                                            Authentication authentication) {
        log.info("=== PRODUCT VIEW REQUEST ===");
        log.info("Payload: {}", payload);
        log.info("Authentication: {}", authentication);
        log.info("Authentication name: {}", authentication != null ? authentication.getName() : "null");

        // 1. Check authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Unauthenticated request");
            return ResponseEntity.status(401).body("Unauthorized");
        }

        // 2. Extract articleId
        Long articleId = payload.get("articleId");
        if (articleId == null) {
            log.warn("Missing articleId");
            return ResponseEntity.badRequest().body("Missing articleId");
        }

        // 3. Find user
        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null) {
            log.warn("User not found for email: {}", authentication.getName());
            return ResponseEntity.status(401).body("User not found");
        }
        log.info("User found: {} (id={})", user.getEmail(), user.getId());

        // 4. Find article
        Article article = articleRepository.findById(articleId).orElse(null);
        if (article == null) {
            log.warn("Article not found: {}", articleId);
            return ResponseEntity.notFound().build();
        }
        log.info("Article found: {} (id={})", article.getNom(), article.getId());

        // 5. Save view
        ProductView view = new ProductView();
        view.setUser(user);
        view.setArticle(article);
        view.setViewedAt(LocalDateTime.now());

        productViewRepository.save(view);
        log.info("✅ Product view saved successfully for user {} article {}", user.getId(), articleId);

        return ResponseEntity.ok("View recorded");
    }
}