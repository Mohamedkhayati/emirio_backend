package com.emirio.catalog.repo;

import com.emirio.catalog.VariationArticle;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VariationRepository extends JpaRepository<VariationArticle, Long> {

  @EntityGraph(attributePaths = {"article", "couleur", "taille"})
  List<VariationArticle> findByArticleId(Long articleId);
}
