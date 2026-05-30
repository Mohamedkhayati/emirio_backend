package com.emirio.catalog.repo;

import com.emirio.catalog.Article;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    // SIMPLIFIED QUERIES - load articles only, no JOIN FETCH
    @EntityGraph(attributePaths = "categorie")
    @Query("SELECT a FROM Article a WHERE a.actif = true ORDER BY a.id DESC")
    List<Article> findAllActiveArticles();
    
    @EntityGraph(attributePaths = "categorie")
    @Query("SELECT a FROM Article a WHERE a.categorie.id = :categorieId AND a.actif = true ORDER BY a.id DESC")
    List<Article> findActiveByCategorieId(@Param("categorieId") Long categorieId);
    
    @EntityGraph(attributePaths = "categorie")
    @Query("SELECT a FROM Article a WHERE a.id = :id")
    Optional<Article> findArticleById(@Param("id") Long id);
    
    // Keep your existing methods
    @EntityGraph(attributePaths = "categorie")
    List<Article> findAllByOrderByIdDesc();
    
    @EntityGraph(attributePaths = "categorie")
    List<Article> findByVendeurIdOrderByIdDesc(Long vendeurId);

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
    @Query("select a from Article a where a.categorie.id = :categoryId and a.actif = true order by a.id desc")
    List<Article> findByCategorieId(Long categoryId);

    @EntityGraph(attributePaths = "categorie")
    @Query("select a from Article a where a.actif = true order by a.id asc")
    List<Article> findOldArticles();

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);
}