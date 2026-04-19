package com.emirio.catalog;

import com.emirio.catalog.repo.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CategorySeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    public CategorySeeder(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (categoryRepository.count() > 0) {
            return;
        }

        // ===== MAIN CATEGORIES (Level 1) =====
        Category chaussures = createAndSaveCategory("CHAUSSURES", "Chaussures pour toute la famille", 
            null, CategoryLevel.MAIN, MainCategory.CHAUSSURES, 1, null, true);
        
        Category accessoires = createAndSaveCategory("ACCESSOIRES", "Accessoires de mode", 
            null, CategoryLevel.MAIN, MainCategory.ACCESSOIRES, 2, null, true);

        // ===== SUB CATEGORIES for CHAUSSURES (Level 2) =====
        Category homme = createAndSaveCategory("HOMME", "Chaussures pour hommes", 
            chaussures, CategoryLevel.SUB, MainCategory.CHAUSSURES, 1, null, true);
        Category femme = createAndSaveCategory("FEMME", "Chaussures pour femmes", 
            chaussures, CategoryLevel.SUB, MainCategory.CHAUSSURES, 2, null, true);
        Category kids = createAndSaveCategory("KIDS", "Chaussures pour enfants", 
            chaussures, CategoryLevel.SUB, MainCategory.CHAUSSURES, 3, null, true);
        Category unisex = createAndSaveCategory("UNISEX", "Chaussures unisexes", 
            chaussures, CategoryLevel.SUB, MainCategory.CHAUSSURES, 4, null, true);

        // ===== SUB-SUB CATEGORIES for HOMME (Level 3) =====
        createAndSaveCategory("SPORT", "Chaussures de sport pour hommes", 
            homme, CategoryLevel.SUB_SUB, MainCategory.CHAUSSURES, 1, null, true);
        createAndSaveCategory("CASUAL", "Chaussures casual pour hommes", 
            homme, CategoryLevel.SUB_SUB, MainCategory.CHAUSSURES, 2, null, true);
        createAndSaveCategory("CLASSIQUE", "Chaussures classiques pour hommes", 
            homme, CategoryLevel.SUB_SUB, MainCategory.CHAUSSURES, 3, null, true);
        createAndSaveCategory("CLAQUETTES / SANDALES", "Claquettes et sandales pour hommes", 
            homme, CategoryLevel.SUB_SUB, MainCategory.CHAUSSURES, 4, null, true);

        // ===== SUB-SUB CATEGORIES for FEMME (Level 3) =====
        createAndSaveCategory("SPORT", "Chaussures de sport pour femmes", 
            femme, CategoryLevel.SUB_SUB, MainCategory.CHAUSSURES, 1, null, true);
        createAndSaveCategory("TALONS", "Chaussures à talons pour femmes", 
            femme, CategoryLevel.SUB_SUB, MainCategory.CHAUSSURES, 2, null, true);
        createAndSaveCategory("CASUAL", "Chaussures casual pour femmes", 
            femme, CategoryLevel.SUB_SUB, MainCategory.CHAUSSURES, 3, null, true);
        createAndSaveCategory("BOTTES / BOTTINES", "Bottes et bottines pour femmes", 
            femme, CategoryLevel.SUB_SUB, MainCategory.CHAUSSURES, 4, null, true);
        createAndSaveCategory("SANDALES", "Sandales pour femmes", 
            femme, CategoryLevel.SUB_SUB, MainCategory.CHAUSSURES, 5, null, true);

        // ===== SUB-SUB CATEGORIES for KIDS (Level 3) =====
        createAndSaveCategory("GARCON", "Chaussures pour garçons", 
            kids, CategoryLevel.SUB_SUB, MainCategory.CHAUSSURES, 1, null, true);
        createAndSaveCategory("FILLE", "Chaussures pour filles", 
            kids, CategoryLevel.SUB_SUB, MainCategory.CHAUSSURES, 2, null, true);

        // ===== SUB-SUB CATEGORIES for UNISEX (Level 3) =====
        createAndSaveCategory("SPORT", "Chaussures de sport unisexes", 
            unisex, CategoryLevel.SUB_SUB, MainCategory.CHAUSSURES, 1, null, true);
        createAndSaveCategory("CASUAL", "Chaussures casual unisexes", 
            unisex, CategoryLevel.SUB_SUB, MainCategory.CHAUSSURES, 2, null, true);
        createAndSaveCategory("CLASSIQUE", "Chaussures classiques unisexes", 
            unisex, CategoryLevel.SUB_SUB, MainCategory.CHAUSSURES, 3, null, true);

        // ===== SUB CATEGORIES for ACCESSOIRES (Level 2 - no Level 3 needed) =====
        createAndSaveCategory("SAC A MAIN", "Sacs à main pour toutes occasions", 
            accessoires, CategoryLevel.SUB, MainCategory.ACCESSOIRES, 1, null, true);
        createAndSaveCategory("POCHETTE DE SOIRÉE", "Pochettes élégantes pour soirées", 
            accessoires, CategoryLevel.SUB, MainCategory.ACCESSOIRES, 2, null, true);
    }

    private Category createAndSaveCategory(String nom, String description, Category parent, 
                                            CategoryLevel level, MainCategory mainCategory, 
                                            int order, String iconUrl, boolean actif) {
        // Check if category already exists using parent ID only if parent is saved
        Long parentId = parent != null ? parent.getId() : null;
        
        if (parentId != null && categoryRepository.existsByNomAndParentId(nom, parentId)) {
            return null;
        }
        
        // For top-level categories, check without parent ID condition
        if (parentId == null && categoryRepository.existsByNomAndParentIdIsNull(nom)) {
            return null;
        }
        
        Category c = new Category();
        c.setNom(nom);
        c.setDescription(description);
        c.setParent(parent);
        c.setLevel(level);
        c.setMainCategory(mainCategory);
        c.setDisplayOrder(order);
        c.setIconUrl(iconUrl);
        c.setActif(actif);
        return categoryRepository.save(c);
    }
}