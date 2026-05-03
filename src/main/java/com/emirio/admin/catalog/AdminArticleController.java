package com.emirio.admin.catalog;

import com.emirio.admin.catalog.history.CatalogHistoryService;
import com.emirio.catalog.Article;
import com.emirio.catalog.Category;
import com.emirio.catalog.VariationArticle;
import com.emirio.catalog.repo.ArticleRepository;
import com.emirio.catalog.repo.CategoryRepository;
import com.emirio.catalog.repo.VariationRepository;
import com.emirio.notification.EmailService;
import com.emirio.security.CurrentUserService;
import com.emirio.user.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/admin/articles")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AdminArticleController {

    private final ArticleRepository articles;
    private final CategoryRepository categories;
    private final VariationRepository variationRepository;
    private final CurrentUserService currentUserService;
    private final CatalogHistoryService catalogHistoryService;
    private final EmailService emailService;
    
    @PersistenceContext
    private EntityManager entityManager;

    public AdminArticleController(
        ArticleRepository articles,
        CategoryRepository categories,
        VariationRepository variationRepository,
        CurrentUserService currentUserService,
        CatalogHistoryService catalogHistoryService,
        EmailService emailService
    ) {
        this.articles = articles;
        this.categories = categories;
        this.variationRepository = variationRepository;
        this.currentUserService = currentUserService;
        this.catalogHistoryService = catalogHistoryService;
        this.emailService = emailService;
    }

    @GetMapping
    public List<ArticleDto> list() {
        return articles.findAllByOrderByIdDesc().stream().map(ArticleDto::from).toList();
    }

    @GetMapping("/{id}")
    public ArticleDto details(@PathVariable Long id) {
        return ArticleDto.from(findArticle(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ArticleDto create(
        @RequestPart("data") @Valid CreateReq req,
        @RequestPart(value = "image1", required = false) MultipartFile image1,
        @RequestPart(value = "image2", required = false) MultipartFile image2,
        @RequestPart(value = "image3", required = false) MultipartFile image3,
        @RequestPart(value = "image4", required = false) MultipartFile image4
    ) throws IOException {
        validateArticleRequest(req, null);

        Category cat = findCategory(req.getCategorieId());
        Article article = new Article();
        fill(article, req, cat, image1, image2, image3, image4);

        Article saved = articles.save(article);

        User actor = currentUserService.requireCurrentUser();
        catalogHistoryService.articleCreated(saved, actor);

        String imageUrl = saved.getImageData1() != null ? "/api/articles/" + saved.getId() + "/image/1" : null;
        emailService.sendNewArticleNotification(saved.getNom(), saved.getDescription(), imageUrl);

        return ArticleDto.from(saved);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ArticleDto update(
        @PathVariable Long id,
        @RequestPart("data") @Valid CreateReq req,
        @RequestPart(value = "image1", required = false) MultipartFile image1,
        @RequestPart(value = "image2", required = false) MultipartFile image2,
        @RequestPart(value = "image3", required = false) MultipartFile image3,
        @RequestPart(value = "image4", required = false) MultipartFile image4
    ) throws IOException {
        validateArticleRequest(req, id);

        Category cat = findCategory(req.getCategorieId());
        Article article = findArticle(id);
        fill(article, req, cat, image1, image2, image3, image4);

        Article saved = articles.save(article);

        User actor = currentUserService.requireCurrentUser();
        catalogHistoryService.articleUpdated(saved, actor);

        return ArticleDto.from(saved);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public void delete(@PathVariable Long id) {
        Article article = findArticle(id);
        
        try {
            User actor = currentUserService.requireCurrentUser();
            
            // 1. Delete user recommendations first
            entityManager.createNativeQuery("DELETE FROM user_recommendation WHERE article_id = ?")
                .setParameter(1, id)
                .executeUpdate();
            
            // 2. Delete variation images
            entityManager.createNativeQuery("DELETE vi FROM variation_image vi " +
                                            "INNER JOIN variation_article v ON vi.variation_id = v.id " +
                                            "WHERE v.article_id = ?")
                .setParameter(1, id)
                .executeUpdate();
            
            // 3. Delete variations
            entityManager.createNativeQuery("DELETE FROM variation_article WHERE article_id = ?")
                .setParameter(1, id)
                .executeUpdate();
            
            // 4. Delete product views
            entityManager.createNativeQuery("DELETE FROM product_view WHERE article_id = ?")
                .setParameter(1, id)
                .executeUpdate();
            
            // 5. Record article deletion history
            catalogHistoryService.articleDeleted(article, actor);
            
            // 6. Finally delete the article
            entityManager.createNativeQuery("DELETE FROM article WHERE id = ?")
                .setParameter(1, id)
                .executeUpdate();
            
        } catch (Exception e) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to delete article: " + e.getMessage()
            );
        }
    }

    private Article findArticle(Long id) {
        return articles.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Article not found"));
    }

    private Category findCategory(Long id) {
        return categories.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Category not found"));
    }

    private void validateArticleRequest(CreateReq req, Long articleId) {
        if (req.getCategorieId() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Category is required");
        }

        if (req.getPrix() <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "Price must be greater than 0");
        }

        if (req.getSalePrice() != null) {
            if (req.getSalePrice() <= 0) {
                throw new ResponseStatusException(BAD_REQUEST, "Sale price must be greater than 0");
            }
            if (req.getSalePrice() >= req.getPrix()) {
                throw new ResponseStatusException(BAD_REQUEST, "Sale price must be lower than price");
            }
        }

        if (req.getSaleStartAt() != null
            && req.getSaleEndAt() != null
            && req.getSaleEndAt().isBefore(req.getSaleStartAt())) {
            throw new ResponseStatusException(BAD_REQUEST, "Sale end date must be after sale start date");
        }

        String sku = normalize(req.getSku());
        if (sku != null) {
            boolean exists = articleId == null
                ? articles.existsBySkuIgnoreCase(sku)
                : articles.existsBySkuIgnoreCaseAndIdNot(sku, articleId);

            if (exists) {
                throw new ResponseStatusException(BAD_REQUEST, "SKU already exists");
            }
        }
    }

    private String normalize(String value) {
        if (value == null) return null;
        String v = value.trim();
        return v.isEmpty() ? null : v;
    }

    private void fill(
        Article article,
        CreateReq req,
        Category category,
        MultipartFile image1,
        MultipartFile image2,
        MultipartFile image3,
        MultipartFile image4
    ) throws IOException {
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

        applyImage(article, 1, image1);
        applyImage(article, 2, image2);
        applyImage(article, 3, image3);
        applyImage(article, 4, image4);
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String v = value.trim();
        return v.isEmpty() ? null : v;
    }

    private void applyImage(Article article, int index, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return;
        }

        if (index == 1) {
            article.setImageData1(file.getBytes());
            article.setImageName1(file.getOriginalFilename());
            article.setImageType1(file.getContentType());
        } else if (index == 2) {
            article.setImageData2(file.getBytes());
            article.setImageName2(file.getOriginalFilename());
            article.setImageType2(file.getContentType());
        } else if (index == 3) {
            article.setImageData3(file.getBytes());
            article.setImageName3(file.getOriginalFilename());
            article.setImageType3(file.getContentType());
        } else if (index == 4) {
            article.setImageData4(file.getBytes());
            article.setImageName4(file.getOriginalFilename());
            article.setImageType4(file.getContentType());
        }
    }

    @Data
    public static class CreateReq {
        @NotBlank
        @Size(max = 180)
        private String nom;

        @Size(max = 2000)
        private String description;

        @Size(max = 6000)
        private String details;

        @DecimalMin(value = "0.001")
        private double prix;

        private boolean actif = true;

        @NotNull
        private Long categorieId;

        @Size(max = 160)
        private String marque;

        @Size(max = 160)
        private String matiere;

        @Size(max = 120)
        private String sku;

        private Double salePrice;
        private LocalDateTime saleStartAt;
        private LocalDateTime saleEndAt;
        private boolean recommended;
    }

    @Data
    public static class ArticleDto {
        private Long id;
        private String nom;
        private String description;
        private String details;
        private double prix;
        private boolean actif;
        private Long categorieId;
        private String categorieNom;
        private String marque;
        private String matiere;
        private String sku;
        private String imageUrl;
        private Double salePrice;
        private LocalDateTime saleStartAt;
        private LocalDateTime saleEndAt;
        private boolean recommended;

        static ArticleDto from(Article article) {
            ArticleDto dto = new ArticleDto();
            dto.id = article.getId();
            dto.nom = article.getNom();
            dto.description = article.getDescription();
            dto.details = article.getDetails();
            dto.prix = article.getPrix();
            dto.actif = article.isActif();
            dto.categorieId = article.getCategorie() != null ? article.getCategorie().getId() : null;
            dto.categorieNom = article.getCategorie() != null ? article.getCategorie().getNom() : null;
            dto.marque = article.getMarque();
            dto.matiere = article.getMatiere();
            dto.sku = article.getSku();
            dto.imageUrl = article.getImageData1() != null ? "/api/articles/" + article.getId() + "/image/1" : null;
            dto.salePrice = article.getSalePrice();
            dto.saleStartAt = article.getSaleStartAt();
            dto.saleEndAt = article.getSaleEndAt();
            dto.recommended = article.isRecommended();
            return dto;
        }
    }
}