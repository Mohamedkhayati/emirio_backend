package com.emirio.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RecommendationResponse {
    private Long articleId;
    private String articleName;
    private double score;
    private String imageUrl;
    private double price;
}