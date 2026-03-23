package com.emirio.catalog;

import com.emirio.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "article_review")
public class ArticleReview {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "article_id")
  private Article article;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(nullable = false)
  private int rating;

  @Column(nullable = false, length = 2000)
  private String comment;

  @Column(nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();
}
