package com.emirio.admin.catalog.history;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CatalogHistoryRepository extends JpaRepository<CatalogHistory, Long> {
    List<CatalogHistory> findAllByOrderByActionAtDesc();
    List<CatalogHistory> findByArticleIdOrderByActionAtDesc(Long articleId);
    List<CatalogHistory> findByVariationIdOrderByActionAtDesc(Long variationId);
}