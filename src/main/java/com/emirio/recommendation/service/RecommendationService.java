package com.emirio.recommendation.service;

import com.emirio.recommendation.dto.RecommendationResponse;
import com.emirio.recommendation.entity.UserRecommendation;
import com.emirio.recommendation.repository.UserRecommendationRepository;
import com.emirio.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final UserRecommendationRepository userRecommendationRepository;

    public RecommendationService(UserRecommendationRepository userRecommendationRepository) {
        this.userRecommendationRepository = userRecommendationRepository;
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendations(User user, int limit) {
        List<UserRecommendation> recs = userRecommendationRepository.findByUserOrderByRankAsc(user);
        if (recs.isEmpty()) {
            return List.of();
        }
        return recs.stream()
                .limit(limit)
                .map(r -> new RecommendationResponse(
                        r.getArticle().getId(),
                        r.getArticle().getNom(),
                        r.getScore(),
                        r.getArticle().getImageUrl1(),
                        r.getArticle().getPrix()))
                .collect(Collectors.toList());
    }
}