package com.emirio.catalog.api;

import com.emirio.catalog.Category;
import com.emirio.catalog.CategoryLevel;
import com.emirio.catalog.MainCategory;
import com.emirio.catalog.repo.CategoryRepository;
import lombok.Data;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class PublicCategoryController {

    private final CategoryRepository repo;

    public PublicCategoryController(CategoryRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public CategoryStructureDto getCategoryStructure() {
        CategoryStructureDto structure = new CategoryStructureDto();
        
        // Get CHAUSSURES main category and its structure
        List<Category> chaussuresSubs = repo.findByMainCategoryAndLevel(MainCategory.CHAUSSURES, CategoryLevel.SUB);
        Map<String, CategoryHierarchyDto> chaussuresMap = new LinkedHashMap<>();
        
        for (Category sub : chaussuresSubs) {
            CategoryHierarchyDto subDto = new CategoryHierarchyDto();
            subDto.setId(sub.getId());
            subDto.setNom(sub.getNom());
            
            // Get sub-sub categories for this sub
            List<Category> subSubs = repo.findByParentIdOrderByDisplayOrderAsc(sub.getId());
            subDto.setChildren(subSubs.stream()
                .map(ss -> {
                    CategoryDto dto = new CategoryDto();
                    dto.setId(ss.getId());
                    dto.setNom(ss.getNom());
                    dto.setDescription(ss.getDescription());
                    return dto;
                })
                .collect(Collectors.toList()));
            
            chaussuresMap.put(sub.getNom().toLowerCase(), subDto);
        }
        structure.setChaussures(chaussuresMap);
        
        // Get ACCESSOIRES structure
        List<Category> accessoiresSubs = repo.findByMainCategoryAndLevel(MainCategory.ACCESSOIRES, CategoryLevel.SUB);
        structure.setAccessoires(accessoiresSubs.stream()
            .map(sub -> {
                CategoryDto dto = new CategoryDto();
                dto.setId(sub.getId());
                dto.setNom(sub.getNom());
                dto.setDescription(sub.getDescription());
                return dto;
            })
            .collect(Collectors.toList()));
        
        return structure;
    }

    @GetMapping("/main")
    public List<CategoryDto> getMainCategories() {
        return repo.findByLevelOrderByDisplayOrderAsc(CategoryLevel.MAIN).stream()
            .map(CategoryDto::from)
            .collect(Collectors.toList());
    }

    @GetMapping("/main/{mainCategory}/sub")
    public List<CategoryDto> getSubCategories(@PathVariable MainCategory mainCategory) {
        return repo.findByMainCategoryAndLevel(mainCategory, CategoryLevel.SUB).stream()
            .map(CategoryDto::from)
            .collect(Collectors.toList());
    }

    @GetMapping("/parent/{parentId}/children")
    public List<CategoryDto> getChildren(@PathVariable Long parentId) {
        return repo.findByParentIdOrderByDisplayOrderAsc(parentId).stream()
            .map(CategoryDto::from)
            .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public CategoryDto getCategory(@PathVariable Long id) {
        Category category = repo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Category not found"));
        return CategoryDto.from(category);
    }

    @Data
    public static class CategoryDto {
        private Long id;
        private String nom;
        private String description;
        private Long parentId;
        private String parentNom;
        private String level;
        private String mainCategory;
        private Integer displayOrder;
        private String iconUrl;
        private boolean actif;

        static CategoryDto from(Category c) {
            CategoryDto d = new CategoryDto();
            d.id = c.getId();
            d.nom = c.getNom();
            d.description = c.getDescription();
            d.parentId = c.getParent() != null ? c.getParent().getId() : null;
            d.parentNom = c.getParent() != null ? c.getParent().getNom() : null;
            d.level = c.getLevel() != null ? c.getLevel().name() : null;
            d.mainCategory = c.getMainCategory() != null ? c.getMainCategory().name() : null;
            d.displayOrder = c.getDisplayOrder();
            d.iconUrl = c.getIconUrl();
            d.actif = c.isActif();
            return d;
        }
    }

    @Data
    public static class CategoryHierarchyDto {
        private Long id;
        private String nom;
        private List<CategoryDto> children = new ArrayList<>();
    }

    @Data
    public static class CategoryStructureDto {
        private Map<String, CategoryHierarchyDto> chaussures = new LinkedHashMap<>();
        private List<CategoryDto> accessoires = new ArrayList<>();
    }
}