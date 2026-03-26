package com.emirio.recommendation;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recommendation_config")
public class RecommendationConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private RecommendationType strategy = RecommendationType.HYBRID;

    private Integer favoriteWeight = 5;
    private Integer clickWeight = 3;
    private Integer oldArticleWeight = 1;
    private Integer bestSellerWeight = 4;
    private Integer oldArticleDays = 120;
    private Integer limitCount = 12;

    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public RecommendationType getStrategy() { return strategy; }
    public Integer getFavoriteWeight() { return favoriteWeight; }
    public Integer getClickWeight() { return clickWeight; }
    public Integer getOldArticleWeight() { return oldArticleWeight; }
    public Integer getBestSellerWeight() { return bestSellerWeight; }
    public Integer getOldArticleDays() { return oldArticleDays; }
    public Integer getLimitCount() { return limitCount; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setId(Long id) { this.id = id; }
    public void setStrategy(RecommendationType strategy) { this.strategy = strategy; }
    public void setFavoriteWeight(Integer favoriteWeight) { this.favoriteWeight = favoriteWeight; }
    public void setClickWeight(Integer clickWeight) { this.clickWeight = clickWeight; }
    public void setOldArticleWeight(Integer oldArticleWeight) { this.oldArticleWeight = oldArticleWeight; }
    public void setBestSellerWeight(Integer bestSellerWeight) { this.bestSellerWeight = bestSellerWeight; }
    public void setOldArticleDays(Integer oldArticleDays) { this.oldArticleDays = oldArticleDays; }
    public void setLimitCount(Integer limitCount) { this.limitCount = limitCount; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}