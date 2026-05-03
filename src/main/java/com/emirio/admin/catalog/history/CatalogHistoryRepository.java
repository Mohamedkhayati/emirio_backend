package com.emirio.admin.catalog.history;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CatalogHistoryRepository extends JpaRepository<CatalogHistory, Long> {
    List<CatalogHistory> findAllByOrderByActionAtDesc();
    
    List<CatalogHistory> findByArticleIdOrderByActionAtDesc(Long articleId);
    
    List<CatalogHistory> findByVariationIdOrderByActionAtDesc(Long variationId);
    
    List<CatalogHistory> findByActionOrderByActionAtDesc(CatalogAction action);
    
    List<CatalogHistory> findByTargetTypeOrderByActionAtDesc(CatalogTargetType targetType);
    
    List<CatalogHistory> findByActionAndTargetTypeOrderByActionAtDesc(CatalogAction action, CatalogTargetType targetType);
    
    // Get recent history with limit
    @Query("SELECT h FROM CatalogHistory h ORDER BY h.actionAt DESC")
    List<CatalogHistory> findRecentHistory(@Param("limit") int limit);
}