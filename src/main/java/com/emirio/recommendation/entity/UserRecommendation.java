package com.emirio.recommendation.entity;

import com.emirio.catalog.Article;
import com.emirio.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_recommendation")
public class UserRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(nullable = false)
    private Double score;

    @Column(nullable = false)
    private Integer rank;

    @Column(name = "computed_at", nullable = false)
    private LocalDateTime computedAt;

    // Constructors
    public UserRecommendation() {}

    public UserRecommendation(User user, Article article, Double score, Integer rank, LocalDateTime computedAt) {
        this.user = user;
        this.article = article;
        this.score = score;
        this.rank = rank;
        this.computedAt = computedAt;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Article getArticle() { return article; }
    public void setArticle(Article article) { this.article = article; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public Integer getRank() { return rank; }
    public void setRank(Integer rank) { this.rank = rank; }
    public LocalDateTime getComputedAt() { return computedAt; }
    public void setComputedAt(LocalDateTime computedAt) { this.computedAt = computedAt; }
}