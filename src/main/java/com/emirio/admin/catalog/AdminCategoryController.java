package com.emirio.admin.catalog;

import com.emirio.catalog.Category;
import com.emirio.catalog.CategoryLevel;
import com.emirio.catalog.MainCategory;
import com.emirio.catalog.repo.CategoryRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/admin/categories")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AdminCategoryController {

    private final CategoryRepository repo;

    public AdminCategoryController(CategoryRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<CategoryDto> list() {
        return repo.findAllByOrderByDisplayOrderAsc().stream()
            .map(CategoryDto::from)
            .collect(Collectors.toList());
    }

    @GetMapping("/tree")
    public List<CategoryNodeDto> getTree() {
        List<Category> rootCategories = repo.findByParentIsNullOrderByDisplayOrderAsc();
        return rootCategories.stream()
            .map(this::buildCategoryTree)
            .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public CategoryDto details(@PathVariable Long id) {
        return CategoryDto.from(findCategory(id));
    }

    @GetMapping("/{id}/children")
    public List<CategoryDto> getChildren(@PathVariable Long id) {
        return repo.findByParentIdOrderByDisplayOrderAsc(id).stream()
            .map(CategoryDto::from)
            .collect(Collectors.toList());
    }

    @PostMapping
    public CategoryDto create(@RequestBody @Valid CreateReq req) {
        String nom = req.getNom().trim();
        
        // Check uniqueness within same parent
        if (categoryExistsInParent(nom, req.getParentId())) {
            throw new ResponseStatusException(BAD_REQUEST, "Category name already exists in this parent");
        }

        Category c = new Category();
        c.setNom(nom);
        c.setDescription(trimToNull(req.getDescription()));
        
        if (req.getParentId() != null) {
            Category parent = findCategory(req.getParentId());
            c.setParent(parent);
        }
        
        c.setDisplayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0);
        
        // Set level based on parent
        if (req.getParentId() == null) {
            c.setLevel(CategoryLevel.MAIN);
        } else {
            Category parent = findCategory(req.getParentId());
            if (parent.getLevel() == CategoryLevel.MAIN) {
                c.setLevel(CategoryLevel.SUB);
            } else if (parent.getLevel() == CategoryLevel.SUB) {
                c.setLevel(CategoryLevel.SUB_SUB);
            } else {
                c.setLevel(CategoryLevel.SUB_SUB);
            }
        }
        
        // Set main category
        if (req.getMainCategory() != null) {
            c.setMainCategory(req.getMainCategory());
        } else if (req.getParentId() != null) {
            Category parent = findCategory(req.getParentId());
            c.setMainCategory(parent.getMainCategory());
        }
        
        c.setIconUrl(req.getIconUrl());
        c.setActif(req.isActif());
        
        return CategoryDto.from(repo.save(c));
    }

    @PutMapping("/{id}")
    public CategoryDto update(@PathVariable Long id, @RequestBody @Valid UpdateReq req) {
        Category c = findCategory(id);
        String nom = req.getNom().trim();

        // Check uniqueness within same parent (excluding current)
        if (categoryExistsInParentExcludingCurrent(nom, req.getParentId(), id)) {
            throw new ResponseStatusException(BAD_REQUEST, "Category name already exists in this parent");
        }

        c.setNom(nom);
        c.setDescription(trimToNull(req.getDescription()));
        
        if (req.getParentId() != null) {
            Category parent = findCategory(req.getParentId());
            // Prevent circular reference
            if (isCircularReference(c, parent)) {
                throw new ResponseStatusException(BAD_REQUEST, "Cannot create circular category reference");
            }
            c.setParent(parent);
            
            // Update level based on new parent
            if (parent.getLevel() == CategoryLevel.MAIN) {
                c.setLevel(CategoryLevel.SUB);
            } else if (parent.getLevel() == CategoryLevel.SUB) {
                c.setLevel(CategoryLevel.SUB_SUB);
            } else {
                c.setLevel(CategoryLevel.SUB_SUB);
            }
            
            // Update main category based on parent
            c.setMainCategory(parent.getMainCategory());
        } else {
            c.setParent(null);
            c.setLevel(CategoryLevel.MAIN);
        }
        
        c.setDisplayOrder(req.getDisplayOrder());
        if (req.getMainCategory() != null) {
            c.setMainCategory(req.getMainCategory());
        }
        c.setIconUrl(req.getIconUrl());
        c.setActif(req.isActif());
        
        return CategoryDto.from(repo.save(c));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        Category c = findCategory(id);
        
        // Check if category has children using repository method
        if (repo.hasChildren(id)) {
            throw new ResponseStatusException(BAD_REQUEST, "Cannot delete category with children. Move or delete children first.");
        }
        
        repo.delete(c);
    }

    @PatchMapping("/{id}/move")
    public CategoryDto moveCategory(@PathVariable Long id, @RequestBody MoveReq req) {
        Category c = findCategory(id);
        
        if (req.getNewParentId() != null) {
            Category newParent = findCategory(req.getNewParentId());
            
            // Prevent circular reference
            if (isCircularReference(c, newParent)) {
                throw new ResponseStatusException(BAD_REQUEST, "Cannot move category to its own descendant");
            }
            
            c.setParent(newParent);
            
            // Update level based on new parent
            if (newParent.getLevel() == CategoryLevel.MAIN) {
                c.setLevel(CategoryLevel.SUB);
            } else if (newParent.getLevel() == CategoryLevel.SUB) {
                c.setLevel(CategoryLevel.SUB_SUB);
            }
            
            // Update main category based on parent
            c.setMainCategory(newParent.getMainCategory());
        } else {
            c.setParent(null);
            c.setLevel(CategoryLevel.MAIN);
        }
        
        if (req.getNewDisplayOrder() != null) {
            c.setDisplayOrder(req.getNewDisplayOrder());
        }
        
        return CategoryDto.from(repo.save(c));
    }

    @PatchMapping("/reorder")
    public void reorderCategories(@RequestBody List<ReorderReq> reorderRequests) {
        for (ReorderReq req : reorderRequests) {
            Category c = findCategory(req.getCategoryId());
            c.setDisplayOrder(req.getNewOrder());
            repo.save(c);
        }
    }

    private Category findCategory(Long id) {
        return repo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Category not found"));
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String v = value.trim();
        return v.isEmpty() ? null : v;
    }

    private boolean categoryExistsInParent(String nom, Long parentId) {
        List<Category> existing = parentId == null 
            ? repo.findByParentIsNullOrderByDisplayOrderAsc()
            : repo.findByParentIdOrderByDisplayOrderAsc(parentId);
        
        return existing.stream()
            .anyMatch(c -> c.getNom().equalsIgnoreCase(nom));
    }

    private boolean categoryExistsInParentExcludingCurrent(String nom, Long parentId, Long excludeId) {
        List<Category> existing = parentId == null 
            ? repo.findByParentIsNullOrderByDisplayOrderAsc()
            : repo.findByParentIdOrderByDisplayOrderAsc(parentId);
        
        return existing.stream()
            .filter(c -> !c.getId().equals(excludeId))
            .anyMatch(c -> c.getNom().equalsIgnoreCase(nom));
    }

    private boolean isCircularReference(Category category, Category newParent) {
        Category current = newParent;
        while (current != null) {
            if (current.getId().equals(category.getId())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private CategoryNodeDto buildCategoryTree(Category category) {
        CategoryNodeDto dto = CategoryNodeDto.from(category);
        
        // Use repository to get children instead of category.getChildren()
        List<Category> children = repo.findByParentIdOrderByDisplayOrderAsc(category.getId());
        dto.setChildren(children.stream()
            .map(this::buildCategoryTree)
            .collect(Collectors.toList()));
        
        return dto;
    }

    @Data
    public static class CreateReq {
        @NotBlank
        @Size(max = 120)
        private String nom;
        
        @Size(max = 1000)
        private String description;
        
        private Long parentId;
        private Integer displayOrder;
        private MainCategory mainCategory;
        private String iconUrl;
        private boolean actif = true;
    }

    @Data
    public static class UpdateReq {
        @NotBlank
        @Size(max = 120)
        private String nom;
        
        @Size(max = 1000)
        private String description;
        
        private Long parentId;
        private Integer displayOrder;
        private MainCategory mainCategory;
        private String iconUrl;
        private boolean actif;
    }

    @Data
    public static class MoveReq {
        private Long newParentId;
        private Integer newDisplayOrder;
    }

    @Data
    public static class ReorderReq {
        private Long categoryId;
        private Integer newOrder;
    }

    @Data
    public static class CategoryDto {
        private Long id;
        private String nom;
        private String description;
        private Long parentId;
        private String parentNom;
        private Integer displayOrder;
        private String level;
        private MainCategory mainCategory;
        private String iconUrl;
        private boolean actif;
        private boolean hasChildren;
        private int childrenCount;

        static CategoryDto from(Category c) {
            CategoryDto d = new CategoryDto();
            d.id = c.getId();
            d.nom = c.getNom();
            d.description = c.getDescription();
            d.parentId = c.getParent() != null ? c.getParent().getId() : null;
            d.parentNom = c.getParent() != null ? c.getParent().getNom() : null;
            d.displayOrder = c.getDisplayOrder();
            d.level = c.getLevel() != null ? c.getLevel().name() : null;
            d.mainCategory = c.getMainCategory();
            d.iconUrl = c.getIconUrl();
            d.actif = c.isActif();
            d.hasChildren = c.hasChildren();
            d.childrenCount = c.getChildrenCount();
            return d;
        }
    }

    @Data
    public static class CategoryNodeDto {
        private Long id;
        private String nom;
        private String description;
        private String level;
        private MainCategory mainCategory;
        private Integer displayOrder;
        private String iconUrl;
        private boolean actif;
        private List<CategoryNodeDto> children = new ArrayList<>();

        static CategoryNodeDto from(Category c) {
            CategoryNodeDto d = new CategoryNodeDto();
            d.id = c.getId();
            d.nom = c.getNom();
            d.description = c.getDescription();
            d.level = c.getLevel() != null ? c.getLevel().name() : null;
            d.mainCategory = c.getMainCategory();
            d.displayOrder = c.getDisplayOrder();
            d.iconUrl = c.getIconUrl();
            d.actif = c.isActif();
            return d;
        }
    }
}