package com.emirio.recommendation.controller;

import com.emirio.recommendation.dto.RecommendationResponse;
import com.emirio.recommendation.service.RecommendationService;
import com.emirio.user.User;
import com.emirio.user.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/recommendations")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final UserRepository userRepository;

    public RecommendationController(RecommendationService recommendationService,
                                    UserRepository userRepository) {
        this.recommendationService = recommendationService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<RecommendationResponse>> getRecommendations(
            Authentication authentication,
            @RequestParam(defaultValue = "10") int limit) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Unauthorized");
        }
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "User not found"));
        List<RecommendationResponse> recommendations = recommendationService.getRecommendations(user, limit);
        return ResponseEntity.ok(recommendations);
    }
}