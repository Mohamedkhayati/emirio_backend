package com.emirio.recommendation;

import com.emirio.catalog.Article;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/me")
    public List<Article> me(Authentication authentication) {
        Long userId = extractUserId(authentication);
        return recommendationService.getRecommendations(userId);
    }

    @PostMapping("/track")
    public void track(@RequestBody InteractionRequest request, Authentication authentication) {
        Long userId = extractUserId(authentication);
        recommendationService.track(userId, request);
    }

    private Long extractUserId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}