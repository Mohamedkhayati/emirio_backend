package com.emirio.recommendation;

public class RecommendationConfigRequest {
    private RecommendationType strategy;
    private Integer favoriteWeight;
    private Integer clickWeight;
    private Integer oldArticleWeight;
    private Integer bestSellerWeight;
    private Integer oldArticleDays;
    private Integer limitCount;

    public RecommendationType getStrategy() { return strategy; }
    public Integer getFavoriteWeight() { return favoriteWeight; }
    public Integer getClickWeight() { return clickWeight; }
    public Integer getOldArticleWeight() { return oldArticleWeight; }
    public Integer getBestSellerWeight() { return bestSellerWeight; }
    public Integer getOldArticleDays() { return oldArticleDays; }
    public Integer getLimitCount() { return limitCount; }

    public void setStrategy(RecommendationType strategy) { this.strategy = strategy; }
    public void setFavoriteWeight(Integer favoriteWeight) { this.favoriteWeight = favoriteWeight; }
    public void setClickWeight(Integer clickWeight) { this.clickWeight = clickWeight; }
    public void setOldArticleWeight(Integer oldArticleWeight) { this.oldArticleWeight = oldArticleWeight; }
    public void setBestSellerWeight(Integer bestSellerWeight) { this.bestSellerWeight = bestSellerWeight; }
    public void setOldArticleDays(Integer oldArticleDays) { this.oldArticleDays = oldArticleDays; }
    public void setLimitCount(Integer limitCount) { this.limitCount = limitCount; }
}