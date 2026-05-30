package com.emirio.catalog.api;

import com.emirio.catalog.Article;
import com.emirio.catalog.ArticleReview;
import com.emirio.catalog.VariationArticle;
import com.emirio.catalog.VariationImage;
import com.emirio.catalog.repo.ArticleRepository;
import com.emirio.catalog.repo.ArticleReviewRepository;
import com.emirio.catalog.repo.VariationImageRepository;
import com.emirio.catalog.repo.VariationRepository;
import com.emirio.user.User;
import com.emirio.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/articles")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class PublicArticleController {

    private final ArticleRepository articles;
    private final VariationRepository variations;
    private final VariationImageRepository variationImages;
    private final ArticleReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public PublicArticleController(
        ArticleRepository articles,
        VariationRepository variations,
        VariationImageRepository variationImages,
        ArticleReviewRepository reviewRepository,
        UserRepository userRepository
    ) {
        this.articles = articles;
        this.variations = variations;
        this.variationImages = variationImages;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
    }

    // OPTIMIZED: Load articles in one query, then load all variations in batch
    @GetMapping
    public List<ArticleListDto> list(@RequestParam(required = false) Long categorieId) {
        long startTime = System.currentTimeMillis();
        
        // Step 1: Load articles
        List<Article> articleList = (categorieId == null)
            ? articles.findAllActiveArticles()
            : articles.findActiveByCategorieId(categorieId);
        
        if (articleList.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Step 2: Load all variations for these articles in ONE batch query
        List<Long> articleIds = articleList.stream().map(Article::getId).collect(Collectors.toList());
        List<VariationArticle> allVariations = variations.findByArticleIdsWithDetails(articleIds);
        
        // Step 3: Group variations by article ID
        Map<Long, List<VariationArticle>> variationsByArticle = allVariations.stream()
            .collect(Collectors.groupingBy(v -> v.getArticle().getId()));
        
        // Step 4: Build DTOs
        List<ArticleListDto> result = articleList.stream()
            .map(a -> ArticleListDto.from(a, variationsByArticle.getOrDefault(a.getId(), Collections.emptyList())))
            .collect(Collectors.toList());
        
        long endTime = System.currentTimeMillis();
        System.out.println("✅ Loaded " + result.size() + " articles with variations in " + (endTime - startTime) + " ms");
        
        return result;
    }

    @GetMapping("/{id}")
    public ArticleDetailsDto details(@PathVariable Long id) {
        long startTime = System.currentTimeMillis();
        
        Article a = articles.findArticleById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Article not found"));
        
        List<VariationArticle> vars = variations.findForApiByArticleId(id);
        
        long endTime = System.currentTimeMillis();
        System.out.println("✅ Loaded article " + id + " in " + (endTime - startTime) + " ms");
        
        return ArticleDetailsDto.from(a, vars);
    }

    @GetMapping("/variation-image/{imageId}")
    public ResponseEntity<byte[]> variationImage(@PathVariable Long imageId) {
        VariationImage image = variationImages.findById(imageId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Image not found"));
        return fileResponse(image.getImageData(), image.getImageType(), true);
    }

    @GetMapping("/variation-model/{variationId}")
    public ResponseEntity<byte[]> variationModel(@PathVariable Long variationId) {
        VariationArticle v = variations.findForApiById(variationId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Variation not found"));
        return fileResponse(v.getModel3dData(), v.getModel3dType(), false);
    }

    @GetMapping("/{id}/image/{index}")
    public ResponseEntity<byte[]> image(@PathVariable Long id, @PathVariable int index) {
        Article a = articles.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Article not found"));

        byte[] data = switch (index) {
            case 1 -> a.getImageData1();
            case 2 -> a.getImageData2();
            case 3 -> a.getImageData3();
            case 4 -> a.getImageData4();
            default -> null;
        };

        String type = switch (index) {
            case 1 -> a.getImageType1();
            case 2 -> a.getImageType2();
            case 3 -> a.getImageType3();
            case 4 -> a.getImageType4();
            default -> null;
        };

        return fileResponse(data, type, true);
    }

    @GetMapping("/{id}/reviews")
    public List<ReviewDto> reviews(@PathVariable Long id) {
        return reviewRepository.findByArticleIdOrderByCreatedAtDesc(id)
            .stream()
            .map(ReviewDto::from)
            .toList();
    }

    @PostMapping("/{id}/reviews")
    public ReviewDto createReview(
        @PathVariable Long id,
        @RequestBody @Valid ReviewCreateReq req,
        Authentication authentication
    ) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Unauthorized");
        }

        Article article = articles.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Article not found"));
        User user = userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Unauthorized"));

        ArticleReview review = new ArticleReview();
        review.setArticle(article);
        review.setUser(user);
        review.setRating(req.getRating());
        review.setComment(req.getComment());

        return ReviewDto.from(reviewRepository.save(review));
    }

    private static ResponseEntity<byte[]> fileResponse(byte[] data, String type, boolean isPublicCache) {
        if (data == null || data.length == 0) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            if (type != null && !type.isBlank()) {
                mediaType = MediaType.parseMediaType(type);
            }
        } catch (Exception ignored) {
        }

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok().contentType(mediaType);

        if (isPublicCache) {
            builder.header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400");
        } else {
            builder.header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0");
        }

        return builder.body(data);
    }

    // ========== DTO Classes (keep as they were) ==========
    @Data
    public static class ReviewCreateReq {
        @Min(1)
        @Max(5)
        private int rating;

        @NotBlank
        @jakarta.validation.constraints.Size(max = 2000)
        private String comment;
    }

    @Data
    public static class ReviewDto {
        private Long id;
        private int rating;
        private String comment;
        private String userFullName;
        private String createdAtText;

        static ReviewDto from(ArticleReview r) {
            ReviewDto d = new ReviewDto();
            d.id = r.getId();
            d.rating = r.getRating();
            d.comment = r.getComment();
            d.userFullName = ((r.getUser().getPrenom() == null ? "" : r.getUser().getPrenom()) + " " +
                (r.getUser().getNom() == null ? "" : r.getUser().getNom())).trim();
            d.createdAtText = r.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
            return d;
        }
    }

    @Data
    public static class ArticleListDto {
        private Long id;
        private String nom;
        private String description;
        private double prix;
        private boolean actif;
        private Long categorieId;
        private String categorieNom;
        private String marque;
        private String matiere;
        private String sku;
        private String imageUrl;
        private String imageUrl2;
        private String imageUrl3;
        private String imageUrl4;
        private Double salePrice;
        private LocalDateTime saleStartAt;
        private LocalDateTime saleEndAt;
        private boolean recommended;
        private List<VariationDto> variations;
        private List<ColorDto> colors;

        static ArticleListDto from(Article a, List<VariationArticle> vars) {
            ArticleListDto d = new ArticleListDto();
            d.id = a.getId();
            d.nom = a.getNom();
            d.description = a.getDescription();
            d.prix = a.getPrix();
            d.actif = a.isActif();
            d.categorieId = a.getCategorie() != null ? a.getCategorie().getId() : null;
            d.categorieNom = a.getCategorie() != null ? a.getCategorie().getNom() : null;
            d.marque = a.getMarque();
            d.matiere = a.getMatiere();
            d.sku = a.getSku();
            d.salePrice = a.getSalePrice();
            d.saleStartAt = a.getSaleStartAt();
            d.saleEndAt = a.getSaleEndAt();
            d.recommended = a.isRecommended();
            d.variations = vars.stream().map(VariationDto::from).toList();
            d.colors = colorDtos(vars);

            d.imageUrl = articleImageUrl(a, 1);
            d.imageUrl2 = articleImageUrl(a, 2);
            d.imageUrl3 = articleImageUrl(a, 3);
            d.imageUrl4 = articleImageUrl(a, 4);

            return d;
        }
    }

    @Data
    public static class ArticleDetailsDto {
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
        private String imageUrl2;
        private String imageUrl3;
        private String imageUrl4;
        private Double salePrice;
        private LocalDateTime saleStartAt;
        private LocalDateTime saleEndAt;
        private boolean recommended;
        private List<VariationDto> variations;
        private List<ColorDto> colors;

        static ArticleDetailsDto from(Article a, List<VariationArticle> vars) {
            ArticleDetailsDto d = new ArticleDetailsDto();
            d.id = a.getId();
            d.nom = a.getNom();
            d.description = a.getDescription();
            d.details = a.getDetails();
            d.prix = a.getPrix();
            d.actif = a.isActif();
            d.categorieId = a.getCategorie() != null ? a.getCategorie().getId() : null;
            d.categorieNom = a.getCategorie() != null ? a.getCategorie().getNom() : null;
            d.marque = a.getMarque();
            d.matiere = a.getMatiere();
            d.sku = a.getSku();
            d.salePrice = a.getSalePrice();
            d.saleStartAt = a.getSaleStartAt();
            d.saleEndAt = a.getSaleEndAt();
            d.recommended = a.isRecommended();
            d.variations = vars.stream().map(VariationDto::from).toList();
            d.colors = colorDtos(vars);

            d.imageUrl = articleImageUrl(a, 1);
            d.imageUrl2 = articleImageUrl(a, 2);
            d.imageUrl3 = articleImageUrl(a, 3);
            d.imageUrl4 = articleImageUrl(a, 4);

            return d;
        }
    }

    @Data
    public static class VariationDto {
        private Long id;
        private int quantiteStock;
        private double prix;
        private Long couleurId;
        private String couleurNom;
        private String couleurCodeHex;
        private Long tailleId;
        private String taillePointure;
        private List<String> imageUrls;
        private String model3dUrl;

        static VariationDto from(VariationArticle v) {
            VariationDto d = new VariationDto();
            d.id = v.getId();
            d.quantiteStock = v.getQuantiteStock();
            d.prix = v.getPrix();
            d.couleurId = v.getCouleur().getId();
            d.couleurNom = v.getCouleur().getNom();
            d.couleurCodeHex = v.getCouleur().getCodeHex();
            d.tailleId = v.getTaille() != null ? v.getTaille().getId() : null;
            d.taillePointure = v.getTaille() != null ? v.getTaille().getPointure() : null;
            d.imageUrls = variationImageUrls(v);
            d.model3dUrl = variationModelUrl(v);
            return d;
        }
    }

    @Data
    public static class ColorDto {
        private Long couleurId;
        private String couleurNom;
        private String couleurCodeHex;
        private int totalStock;
        private List<String> sizes;
        private String previewImage;
        private String previewModel3dUrl;

        static ColorDto from(List<VariationArticle> sameColor) {
            VariationArticle first = sameColor.get(0);
            ColorDto d = new ColorDto();
            d.couleurId = first.getCouleur().getId();
            d.couleurNom = first.getCouleur().getNom();
            d.couleurCodeHex = first.getCouleur().getCodeHex();
            d.totalStock = sameColor.stream().mapToInt(VariationArticle::getQuantiteStock).sum();
            d.sizes = sameColor.stream()
                .map(v -> v.getTaille() != null ? v.getTaille().getPointure() : null)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .toList();
            d.previewImage = firstPreviewImage(sameColor);
            d.previewModel3dUrl = variationModelUrl(first);
            return d;
        }
    }

    private static List<ColorDto> colorDtos(List<VariationArticle> vars) {
        Map<Long, List<VariationArticle>> grouped = new LinkedHashMap<>();
        for (VariationArticle v : vars) {
            grouped.computeIfAbsent(v.getCouleur().getId(), k -> new ArrayList<>()).add(v);
        }
        return grouped.values().stream().map(ColorDto::from).toList();
    }

    private static String articleImageUrl(Article a, int index) {
        byte[] data = switch (index) {
            case 1 -> a.getImageData1();
            case 2 -> a.getImageData2();
            case 3 -> a.getImageData3();
            case 4 -> a.getImageData4();
            default -> null;
        };
        return data != null && data.length > 0 ? "/api/articles/" + a.getId() + "/image/" + index : null;
    }

    private static List<String> variationImageUrls(VariationArticle v) {
        return v.getImages().stream()
            .map(img -> "/api/articles/variation-image/" + img.getId())
            .toList();
    }

    private static String variationModelUrl(VariationArticle v) {
        byte[] data = v.getModel3dData();
        return data != null && data.length > 0 ? "/api/articles/variation-model/" + v.getId() : null;
    }

    private static String firstPreviewImage(List<VariationArticle> sameColor) {
        return sameColor.stream()
            .flatMap(v -> v.getImages().stream())
            .findFirst()
            .map(img -> "/api/articles/variation-image/" + img.getId())
            .orElse(null);
    }
}