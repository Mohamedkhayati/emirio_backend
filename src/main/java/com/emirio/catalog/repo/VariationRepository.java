package com.emirio.catalog.repo;

import com.emirio.catalog.VariationArticle;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VariationRepository extends JpaRepository<VariationArticle, Long> {

    @EntityGraph(attributePaths = {"article", "couleur", "taille"})
    List<VariationArticle> findByArticleIdOrderByIdAsc(Long articleId);

    @EntityGraph(attributePaths = {"article", "couleur", "taille"})
    List<VariationArticle> findByArticleId(Long articleId);

    @EntityGraph(attributePaths = {"article", "couleur", "taille"})
    Optional<VariationArticle> findFirstByArticleIdAndCouleurIdOrderByIdAsc(Long articleId, Long couleurId);

    @EntityGraph(attributePaths = {"article", "couleur", "taille"})
    Optional<VariationArticle> findById(Long id);

    boolean existsByArticleIdAndCouleurIdAndTailleId(Long articleId, Long couleurId, Long tailleId);

    boolean existsByArticleIdAndCouleurIdAndTailleIdAndIdNot(Long articleId, Long couleurId, Long tailleId, Long id);
}