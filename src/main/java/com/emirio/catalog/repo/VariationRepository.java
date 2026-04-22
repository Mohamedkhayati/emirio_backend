package com.emirio.catalog.repo;

import com.emirio.catalog.VariationArticle;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VariationRepository extends JpaRepository<VariationArticle, Long> {

    @EntityGraph(attributePaths = {"article", "couleur", "taille", "images"})
    List<VariationArticle> findByArticleIdOrderByIdAsc(Long articleId);

    @EntityGraph(attributePaths = {"article", "couleur", "taille", "images"})
    List<VariationArticle> findByArticleId(Long articleId);

    @EntityGraph(attributePaths = {"article", "couleur", "taille", "images"})
    Optional<VariationArticle> findFirstByArticleIdAndCouleurIdOrderByIdAsc(Long articleId, Long couleurId);

    @EntityGraph(attributePaths = {"article", "couleur", "taille", "images"})
    @Query("select v from VariationArticle v where v.article.id = :articleId order by v.id asc")
    List<VariationArticle> findForApiByArticleId(@Param("articleId") Long articleId);

    @EntityGraph(attributePaths = {"article", "couleur", "taille", "images"})
    @Query("select v from VariationArticle v where v.id = :id")
    Optional<VariationArticle> findForApiById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"article", "article.vendeur", "couleur", "taille", "images"})
    @Query("select v from VariationArticle v where v.id = :id")
    Optional<VariationArticle> findWithArticleAndVendeurById(@Param("id") Long id);
 // Find variations of articles owned by a seller
    @Query("select v from VariationArticle v where v.article.vendeur.id = :vendeurId order by v.id asc")
    List<VariationArticle> findByVendeurId(@Param("vendeurId") Long vendeurId);

    boolean existsByArticleIdAndCouleurIdAndTailleId(Long articleId, Long couleurId, Long tailleId);

    boolean existsByArticleIdAndCouleurIdAndTailleIdAndIdNot(Long articleId, Long couleurId, Long tailleId, Long id);

    boolean existsByArticleIdAndCouleurIdAndTailleIsNull(Long articleId, Long couleurId);

    boolean existsByArticleIdAndCouleurIdAndTailleIsNullAndIdNot(Long articleId, Long couleurId, Long id);

    // Vendor filtering – variations belonging to a vendor's articles
    @EntityGraph(attributePaths = {"article", "couleur", "taille", "images"})
    @Query("select v from VariationArticle v where v.article.vendeur.id = :vendeurId order by v.id asc")
    List<VariationArticle> findByArticleVendeurId(@Param("vendeurId") Long vendeurId);
}