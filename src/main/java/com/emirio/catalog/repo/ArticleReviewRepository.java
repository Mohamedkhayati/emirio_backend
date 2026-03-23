package com.emirio.catalog.repo;

import com.emirio.catalog.ArticleReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArticleReviewRepository extends JpaRepository<ArticleReview, Long> {
  List<ArticleReview> findByArticleIdOrderByCreatedAtDesc(Long articleId);
}
