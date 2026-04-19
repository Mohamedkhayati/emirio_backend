package com.emirio.catalog.repo;

import com.emirio.catalog.Category;
import com.emirio.catalog.CategoryLevel;
import com.emirio.catalog.MainCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    List<Category> findAllByOrderByDisplayOrderAsc();
    boolean existsByNomAndParentIdIsNull(String nom);

    List<Category> findByLevelOrderByDisplayOrderAsc(CategoryLevel level);
    
    List<Category> findByMainCategoryOrderByDisplayOrderAsc(MainCategory mainCategory);
    
    List<Category> findByParentIdOrderByDisplayOrderAsc(Long parentId);
    
    List<Category> findByParentIsNullOrderByDisplayOrderAsc();
    
    @Query("SELECT c FROM Category c WHERE c.mainCategory = :mainCategory AND c.level = :level ORDER BY c.displayOrder ASC")
    List<Category> findByMainCategoryAndLevel(@Param("mainCategory") MainCategory mainCategory, @Param("level") CategoryLevel level);
    
    Optional<Category> findByNomAndParentId(String nom, Long parentId);
    
    boolean existsByNomAndParentId(String nom, Long parentId);
    
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.children WHERE c.id = :id")
    Optional<Category> findByIdWithChildren(@Param("id") Long id);
    
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Category c WHERE c.parent.id = :parentId")
    boolean hasChildren(@Param("parentId") Long parentId);
    
    @Query("SELECT c FROM Category c WHERE c.parent.id = :parentId OR c.parent.parent.id = :parentId")
    List<Category> findAllDescendants(@Param("parentId") Long parentId);
    
}