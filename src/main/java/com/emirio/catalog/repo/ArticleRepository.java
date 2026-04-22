package com.emirio.catalog.repo;

import com.emirio.catalog.Article;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    @EntityGraph(attributePaths = "categorie")
    List<Article> findAllByOrderByIdDesc();
 // Find all articles owned by a seller (vendeur)
    @EntityGraph(attributePaths = "categorie")
    List<Article> findByVendeurIdOrderByIdDesc(Long vendeurId);

    // Check existence of SKU for a seller's article (excluding given id)
    boolean existsBySkuIgnoreCaseAndVendeurIdAndIdNot(String sku, Long vendeurId, Long id);

    List<Article> findByCategorieIdAndActifTrue(Long categorieId);

    @EntityGraph(attributePaths = "categorie")
    List<Article> findByActifTrueOrderByIdDesc();

    @EntityGraph(attributePaths = "categorie")
    List<Article> findByCategorieIdAndActifTrueOrderByIdDesc(Long categorieId);

    @Override
    @EntityGraph(attributePaths = "categorie")
    Optional<Article> findById(Long id);

    @EntityGraph(attributePaths = "categorie")
    @Query("""
        select a
        from Article a
        where a.categorie.id = :categoryId
          and a.actif = true
        order by a.id desc
    """)
    List<Article> findByCategorieId(Long categoryId);

    @EntityGraph(attributePaths = "categorie")
    @Query("""
        select a
        from Article a
        where a.actif = true
        order by a.id asc
    """)
    List<Article> findOldArticles();

    @EntityGraph(attributePaths = "categorie")
    @Query("""
        select a
        from Article a
        where a.categorie.id = :categoryId
          and a.actif = true
        order by a.id desc
    """)
    List<Article> findActiveByCategorieId(Long categoryId);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    default List<Long> findBestSellerArticleIds() {
        return Collections.emptyList();
    }
}