package com.emirio.recommendation;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/recommendation-config")
public class AdminRecommendationController {

    private final RecommendationService recommendationService;

    public AdminRecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public RecommendationConfig getConfig() {
        return recommendationService.getConfig();
    }

    @PutMapping
    public RecommendationConfig save(@RequestBody RecommendationConfigRequest request) {
        return recommendationService.saveConfig(request);
    }
}