package com.emirio.api.admin;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/recommendation-config")
public class DummyRecommendationController {

    @GetMapping
    public Map<String, Object> getConfig() {
        return Map.of(
            "strategy", "HYBRID",
            "favoriteWeight", 5,
            "clickWeight", 3,
            "oldArticleWeight", 1,
            "bestSellerWeight", 4,
            "oldArticleDays", 120,
            "limitCount", 12
        );
    }

    @PutMapping
    public void updateConfig(@RequestBody Map<String, Object> config) {
        // no-op
    }
}