package com.emirio.vendeur.service;

import com.emirio.catalog.*;
import com.emirio.catalog.repo.*;
import com.emirio.security.CurrentUserService;
import com.emirio.user.User;
import com.emirio.vendeur.dto.ArticleVendeurDto;
import com.emirio.vendeur.dto.VariationVendeurDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class VendeurArticleService {

    private final ArticleRepository articleRepository;
    private final VariationRepository variationRepository;
    private final CategoryRepository categoryRepository;
    private final ColorRepository colorRepository;
    private final SizeRepository sizeRepository;
    private final CurrentUserService currentUserService;

    private User currentVendeur() {
        return currentUserService.requireCurrentUser();
    }

    @Transactional(readOnly = true)
    public List<ArticleVendeurDto> getMyArticles() {
        User vendeur = currentVendeur();
        List<Article> articles = articleRepository.findByVendeurIdOrderByIdDesc(vendeur.getId());
        return articles.stream()
                .map(article -> {
                    List<VariationArticle> variations = variationRepository.findByArticleId(article.getId());
                    List<VariationVendeurDto> variationDtos = variations.stream()
                            .map(VariationVendeurDto::from)
                            .collect(Collectors.toList());
                    return ArticleVendeurDto.from(article, variationDtos);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ArticleVendeurDto getMyArticle(Long articleId) {
        User vendeur = currentVendeur();
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Article not found"));
        if (!article.getVendeur().getId().equals(vendeur.getId())) {
            throw new ResponseStatusException(FORBIDDEN, "You do not own this article");
        }
        List<VariationArticle> variations = variationRepository.findByArticleId(article.getId());
        List<VariationVendeurDto> variationDtos = variations.stream()
                .map(VariationVendeurDto::from)
                .collect(Collectors.toList());
        return ArticleVendeurDto.from(article, variationDtos);
    }

    @Transactional
    public ArticleVendeurDto createArticle(ArticleCreateRequest req,
                                           MultipartFile image1,
                                           MultipartFile image2,
                                           MultipartFile image3,
                                           MultipartFile image4) throws IOException {
        User vendeur = currentVendeur();
        Category category = categoryRepository.findById(req.getCategorieId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Category not found"));

        // SKU uniqueness check among seller's articles only
        if (req.getSku() != null && !req.getSku().isBlank()) {
            if (articleRepository.existsBySkuIgnoreCaseAndVendeurIdAndIdNot(req.getSku(), vendeur.getId(), null)) {
                throw new ResponseStatusException(BAD_REQUEST, "SKU already used by one of your articles");
            }
        }

        Article article = new Article();
        fillArticle(article, req, category, vendeur);
        applyImages(article, image1, image2, image3, image4);
        Article saved = articleRepository.save(article);
        return getMyArticle(saved.getId());
    }

    @Transactional
    public ArticleVendeurDto updateArticle(Long articleId, ArticleUpdateRequest req,
                                           MultipartFile image1, MultipartFile image2,
                                           MultipartFile image3, MultipartFile image4) throws IOException {
        User vendeur = currentVendeur();
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Article not found"));
        if (!article.getVendeur().getId().equals(vendeur.getId())) {
            throw new ResponseStatusException(FORBIDDEN, "You do not own this article");
        }

        Category category = categoryRepository.findById(req.getCategorieId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Category not found"));

        if (req.getSku() != null && !req.getSku().isBlank()) {
            if (articleRepository.existsBySkuIgnoreCaseAndVendeurIdAndIdNot(req.getSku(), vendeur.getId(), articleId)) {
                throw new ResponseStatusException(BAD_REQUEST, "SKU already used by another of your articles");
            }
        }

        fillArticle(article, req, category, vendeur);
        applyImages(article, image1, image2, image3, image4);
        Article saved = articleRepository.save(article);
        return getMyArticle(saved.getId());
    }

    @Transactional
    public void deleteArticle(Long articleId) {
        User vendeur = currentVendeur();
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Article not found"));
        if (!article.getVendeur().getId().equals(vendeur.getId())) {
            throw new ResponseStatusException(FORBIDDEN, "You do not own this article");
        }
        articleRepository.delete(article);
    }

    // Variations management
    @Transactional
    public List<VariationVendeurDto> createVariations(Long articleId, VariationCreateRequest req,
                                                      List<MultipartFile> images,
                                                      MultipartFile model3d) throws IOException {
        User vendeur = currentVendeur();
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Article not found"));
        if (!article.getVendeur().getId().equals(vendeur.getId())) {
            throw new ResponseStatusException(FORBIDDEN, "You do not own this article");
        }

        Color color = colorRepository.findById(req.getCouleurId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Color not found"));

        boolean accessory = isAccessory(article);
        List<VariationArticle> savedList = new ArrayList<>();

        if (accessory) {
            // Accessory: only one variation per color, no sizes
            VariationArticle v = new VariationArticle();
            v.setArticle(article);
            v.setCouleur(color);
            v.setTaille(null);
            v.setPrix(req.getPrix());
            v.setQuantiteStock(req.getQuantiteStock());
            attachFiles(v, images, model3d);
            savedList.add(variationRepository.save(v));
        } else {
            if (req.getSizes() == null || req.getSizes().isEmpty()) {
                throw new ResponseStatusException(BAD_REQUEST, "At least one size required");
            }
            for (SizeStockReq sizeReq : req.getSizes()) {
                Size size = sizeRepository.findById(sizeReq.getTailleId())
                        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Size not found"));
                VariationArticle v = new VariationArticle();
                v.setArticle(article);
                v.setCouleur(color);
                v.setTaille(size);
                v.setPrix(req.getPrix());
                v.setQuantiteStock(sizeReq.getQuantiteStock());
                attachFiles(v, images, model3d);
                savedList.add(variationRepository.save(v));
            }
        }
        return savedList.stream().map(VariationVendeurDto::from).collect(Collectors.toList());
    }

    @Transactional
    public VariationVendeurDto updateVariation(Long variationId, VariationUpdateRequest req,
                                               List<MultipartFile> images,
                                               MultipartFile model3d) throws IOException {
        User vendeur = currentVendeur();
        VariationArticle variation = variationRepository.findById(variationId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Variation not found"));
        if (!variation.getArticle().getVendeur().getId().equals(vendeur.getId())) {
            throw new ResponseStatusException(FORBIDDEN, "You do not own this variation");
        }

        Color color = colorRepository.findById(req.getCouleurId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Color not found"));
        variation.setCouleur(color);
        variation.setPrix(req.getPrix());
        variation.setQuantiteStock(req.getQuantiteStock());

        boolean accessory = isAccessory(variation.getArticle());
        if (accessory) {
            variation.setTaille(null);
        } else {
            if (req.getTailleId() == null) {
                throw new ResponseStatusException(BAD_REQUEST, "Size required");
            }
            Size size = sizeRepository.findById(req.getTailleId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Size not found"));
            variation.setTaille(size);
        }
        attachFiles(variation, images, model3d);
        VariationArticle saved = variationRepository.save(variation);
        return VariationVendeurDto.from(saved);
    }

    @Transactional
    public void deleteVariation(Long variationId) {
        User vendeur = currentVendeur();
        VariationArticle variation = variationRepository.findById(variationId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Variation not found"));
        if (!variation.getArticle().getVendeur().getId().equals(vendeur.getId())) {
            throw new ResponseStatusException(FORBIDDEN, "You do not own this variation");
        }
        variationRepository.delete(variation);
    }

    // --- private helpers ---
    private void fillArticle(Article article, ArticleRequest req, Category category, User vendeur) {
        article.setNom(req.getNom().trim());
        article.setDescription(trimToNull(req.getDescription()));
        article.setDetails(trimToNull(req.getDetails()));
        article.setPrix(req.getPrix());
        article.setActif(req.isActif());
        article.setCategorie(category);
        article.setMarque(trimToNull(req.getMarque()));
        article.setMatiere(trimToNull(req.getMatiere()));
        article.setSku(trimToNull(req.getSku()));
        article.setRecommended(req.isRecommended());
        article.setSalePrice(req.getSalePrice());
        article.setSaleStartAt(req.getSaleStartAt());
        article.setSaleEndAt(req.getSaleEndAt());
        article.setVendeur(vendeur);
    }

    private void applyImages(Article article, MultipartFile... images) throws IOException {
        MultipartFile[] files = {images[0], images[1], images[2], images[3]};
        for (int i = 0; i < files.length; i++) {
            if (files[i] != null && !files[i].isEmpty()) {
                byte[] data = files[i].getBytes();
                switch (i) {
                    case 0: article.setImageData1(data); article.setImageName1(files[i].getOriginalFilename()); article.setImageType1(files[i].getContentType()); break;
                    case 1: article.setImageData2(data); article.setImageName2(files[i].getOriginalFilename()); article.setImageType2(files[i].getContentType()); break;
                    case 2: article.setImageData3(data); article.setImageName3(files[i].getOriginalFilename()); article.setImageType3(files[i].getContentType()); break;
                    case 3: article.setImageData4(data); article.setImageName4(files[i].getOriginalFilename()); article.setImageType4(files[i].getContentType()); break;
                }
            }
        }
    }

    private void attachFiles(VariationArticle variation, List<MultipartFile> images, MultipartFile model3d) throws IOException {
        if (images != null && !images.isEmpty()) {
            variation.getImages().clear();
            for (MultipartFile file : images) {
                if (file == null || file.isEmpty()) continue;
                VariationImage img = new VariationImage();
                img.setVariation(variation);
                img.setImageData(file.getBytes());
                img.setImageType(file.getContentType());
                img.setImageName(file.getOriginalFilename());
                variation.getImages().add(img);
            }
        }
        if (model3d != null && !model3d.isEmpty()) {
            variation.setModel3dData(model3d.getBytes());
            variation.setModel3dName(model3d.getOriginalFilename());
            variation.setModel3dType(model3d.getContentType());
        }
    }

    /**
     * Determines if an article is an accessory (no sizes, only stock per color).
     * Checks:
     * - Category's mainCategory enum (if available)
     * - Category name (case-insensitive) for keywords "accessoire", "accessoires", "accessory", "sac a main", "sac à main", "pochette de soirée"
     */
    private boolean isAccessory(Article article) {
        if (article == null || article.getCategorie() == null) return false;
        Category cat = article.getCategorie();

        // Check mainCategory enum (if your Category entity has it)
        if (cat.getMainCategory() != null && cat.getMainCategory() == MainCategory.ACCESSOIRES) {
            return true;
        }

        // Fallback: check category name (case-insensitive, partial match)
        String catName = cat.getNom().trim().toLowerCase();
        return catName.contains("accessoire") ||
               catName.contains("accessoires") ||
               catName.contains("accessory") ||
               catName.contains("sac a main") ||
               catName.contains("sac à main") ||
               catName.contains("pochette de soirée");
    }

    private String trimToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    // --- inner request classes ---
    public interface ArticleRequest {
        String getNom(); double getPrix(); boolean isActif(); Long getCategorieId();
        String getDescription(); String getDetails(); String getMarque(); String getMatiere();
        String getSku(); Double getSalePrice(); LocalDateTime getSaleStartAt(); LocalDateTime getSaleEndAt(); boolean isRecommended();
    }

    public static class ArticleCreateRequest implements ArticleRequest {
        private String nom; private double prix; private boolean actif = true; private Long categorieId;
        private String description; private String details; private String marque; private String matiere;
        private String sku; private Double salePrice; private LocalDateTime saleStartAt; private LocalDateTime saleEndAt; private boolean recommended;
        public String getNom() { return nom; } public void setNom(String nom) { this.nom = nom; }
        public double getPrix() { return prix; } public void setPrix(double prix) { this.prix = prix; }
        public boolean isActif() { return actif; } public void setActif(boolean actif) { this.actif = actif; }
        public Long getCategorieId() { return categorieId; } public void setCategorieId(Long categorieId) { this.categorieId = categorieId; }
        public String getDescription() { return description; } public void setDescription(String description) { this.description = description; }
        public String getDetails() { return details; } public void setDetails(String details) { this.details = details; }
        public String getMarque() { return marque; } public void setMarque(String marque) { this.marque = marque; }
        public String getMatiere() { return matiere; } public void setMatiere(String matiere) { this.matiere = matiere; }
        public String getSku() { return sku; } public void setSku(String sku) { this.sku = sku; }
        public Double getSalePrice() { return salePrice; } public void setSalePrice(Double salePrice) { this.salePrice = salePrice; }
        public LocalDateTime getSaleStartAt() { return saleStartAt; } public void setSaleStartAt(LocalDateTime saleStartAt) { this.saleStartAt = saleStartAt; }
        public LocalDateTime getSaleEndAt() { return saleEndAt; } public void setSaleEndAt(LocalDateTime saleEndAt) { this.saleEndAt = saleEndAt; }
        public boolean isRecommended() { return recommended; } public void setRecommended(boolean recommended) { this.recommended = recommended; }
    }

    public static class ArticleUpdateRequest implements ArticleRequest {
        private String nom; private double prix; private boolean actif; private Long categorieId;
        private String description; private String details; private String marque; private String matiere;
        private String sku; private Double salePrice; private LocalDateTime saleStartAt; private LocalDateTime saleEndAt; private boolean recommended;
        public String getNom() { return nom; } public void setNom(String nom) { this.nom = nom; }
        public double getPrix() { return prix; } public void setPrix(double prix) { this.prix = prix; }
        public boolean isActif() { return actif; } public void setActif(boolean actif) { this.actif = actif; }
        public Long getCategorieId() { return categorieId; } public void setCategorieId(Long categorieId) { this.categorieId = categorieId; }
        public String getDescription() { return description; } public void setDescription(String description) { this.description = description; }
        public String getDetails() { return details; } public void setDetails(String details) { this.details = details; }
        public String getMarque() { return marque; } public void setMarque(String marque) { this.marque = marque; }
        public String getMatiere() { return matiere; } public void setMatiere(String matiere) { this.matiere = matiere; }
        public String getSku() { return sku; } public void setSku(String sku) { this.sku = sku; }
        public Double getSalePrice() { return salePrice; } public void setSalePrice(Double salePrice) { this.salePrice = salePrice; }
        public LocalDateTime getSaleStartAt() { return saleStartAt; } public void setSaleStartAt(LocalDateTime saleStartAt) { this.saleStartAt = saleStartAt; }
        public LocalDateTime getSaleEndAt() { return saleEndAt; } public void setSaleEndAt(LocalDateTime saleEndAt) { this.saleEndAt = saleEndAt; }
        public boolean isRecommended() { return recommended; } public void setRecommended(boolean recommended) { this.recommended = recommended; }
    }

    public static class VariationCreateRequest {
        private Double prix; private Long couleurId; private Integer quantiteStock; private List<SizeStockReq> sizes;
        public Double getPrix() { return prix; } public void setPrix(Double prix) { this.prix = prix; }
        public Long getCouleurId() { return couleurId; } public void setCouleurId(Long couleurId) { this.couleurId = couleurId; }
        public Integer getQuantiteStock() { return quantiteStock; } public void setQuantiteStock(Integer quantiteStock) { this.quantiteStock = quantiteStock; }
        public List<SizeStockReq> getSizes() { return sizes; } public void setSizes(List<SizeStockReq> sizes) { this.sizes = sizes; }
    }

    public static class VariationUpdateRequest {
        private Double prix; private Long couleurId; private Long tailleId; private Integer quantiteStock;
        public Double getPrix() { return prix; } public void setPrix(Double prix) { this.prix = prix; }
        public Long getCouleurId() { return couleurId; } public void setCouleurId(Long couleurId) { this.couleurId = couleurId; }
        public Long getTailleId() { return tailleId; } public void setTailleId(Long tailleId) { this.tailleId = tailleId; }
        public Integer getQuantiteStock() { return quantiteStock; } public void setQuantiteStock(Integer quantiteStock) { this.quantiteStock = quantiteStock; }
    }

    public static class SizeStockReq {
        private Long tailleId; private Integer quantiteStock;
        public Long getTailleId() { return tailleId; } public void setTailleId(Long tailleId) { this.tailleId = tailleId; }
        public Integer getQuantiteStock() { return quantiteStock; } public void setQuantiteStock(Integer quantiteStock) { this.quantiteStock = quantiteStock; }
    }
}