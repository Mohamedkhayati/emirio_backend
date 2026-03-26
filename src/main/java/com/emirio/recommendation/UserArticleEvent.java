package com.emirio.recommendation;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_article_event")
public class UserArticleEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long articleId;
    private Long categoryId;

    @Column(nullable = false, length = 40)
    private String eventType;

    private Integer score = 1;

    private LocalDateTime createdAt = LocalDateTime.now();

    public UserArticleEvent() {
    }

    public UserArticleEvent(Long userId, Long articleId, Long categoryId, String eventType, Integer score) {
        this.userId = userId;
        this.articleId = articleId;
        this.categoryId = categoryId;
        this.eventType = eventType;
        this.score = score;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getArticleId() { return articleId; }
    public Long getCategoryId() { return categoryId; }
    public String getEventType() { return eventType; }
    public Integer getScore() { return score; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setArticleId(Long articleId) { this.articleId = articleId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public void setScore(Integer score) { this.score = score; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}