package com.emirio.recommendation;

public class InteractionRequest {
    private Long articleId;
    private Long categoryId;
    private String eventType;

    public Long getArticleId() { return articleId; }
    public Long getCategoryId() { return categoryId; }
    public String getEventType() { return eventType; }

    public void setArticleId(Long articleId) { this.articleId = articleId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public void setEventType(String eventType) { this.eventType = eventType; }
}