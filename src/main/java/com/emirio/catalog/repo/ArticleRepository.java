package com.emirio.catalog.repo;

import com.emirio.catalog.Article;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {

  @EntityGraph(attributePaths = "categorie")
  List<Article> findAllByOrderByIdDesc();

  @EntityGraph(attributePaths = "categorie")
  List<Article> findByActifTrueOrderByIdDesc();

  @EntityGraph(attributePaths = "categorie")
  List<Article> findByCategorieIdAndActifTrueOrderByIdDesc(Long categorieId);

  @Override
  @EntityGraph(attributePaths = "categorie")
  Optional<Article> findById(Long id);
}
