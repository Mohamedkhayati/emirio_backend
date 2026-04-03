package com.emirio.catalog.api;

import com.emirio.catalog.Article;
import com.emirio.catalog.ArticleReview;
import com.emirio.catalog.VariationArticle;
import com.emirio.catalog.repo.ArticleRepository;
import com.emirio.catalog.repo.ArticleReviewRepository;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/articles")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class PublicArticleController {

  private final ArticleRepository articles;
  private final VariationRepository variations;
  private final ArticleReviewRepository reviewRepository;
  private final UserRepository userRepository;

  public PublicArticleController(
    ArticleRepository articles,
    VariationRepository variations,
    ArticleReviewRepository reviewRepository,
    UserRepository userRepository
  ) {
    this.articles = articles;
    this.variations = variations;
    this.reviewRepository = reviewRepository;
    this.userRepository = userRepository;
  }

  @GetMapping
  public List<ArticleListDto> list(@RequestParam(required = false) Long categorieId) {
    List<Article> list = categorieId == null
      ? articles.findByActifTrueOrderByIdDesc()
      : articles.findByCategorieIdAndActifTrueOrderByIdDesc(categorieId);

    return list.stream().map(a -> {
      List<VariationArticle> vars = variations.findByArticleIdOrderByIdAsc(a.getId());
      return ArticleListDto.from(a, vars);
    }).toList();
  }

  @GetMapping("/{id}")
  public ArticleDetailsDto details(@PathVariable Long id) {
    Article a = articles.findById(id)
      .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Article not found"));
    List<VariationArticle> vars = variations.findByArticleIdOrderByIdAsc(id);
    return ArticleDetailsDto.from(a, vars);
  }

  @GetMapping("/variation-image/{variationId}/{index}")
  public ResponseEntity<byte[]> variationImage(@PathVariable Long variationId, @PathVariable int index) {
    VariationArticle v = variations.findById(variationId)
      .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Variation not found"));

    byte[] data = switch (index) {
      case 1 -> v.getImageData1();
      case 2 -> v.getImageData2();
      case 3 -> v.getImageData3();
      case 4 -> v.getImageData4();
      default -> null;
    };

    String type = switch (index) {
      case 1 -> v.getImageType1();
      case 2 -> v.getImageType2();
      case 3 -> v.getImageType3();
      case 4 -> v.getImageType4();
      default -> null;
    };

    if (data == null || data.length == 0) {
      return ResponseEntity.notFound().build();
    }

    MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
    try {
      if (type != null && !type.isBlank()) mediaType = MediaType.parseMediaType(type);
    } catch (Exception ignored) {
    }

    return ResponseEntity.ok()
    		  .contentType(mediaType)
    		  .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0")
    		  .header(HttpHeaders.PRAGMA, "no-cache")
    		  .header(HttpHeaders.EXPIRES, "0")
    		  .body(data);
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

    if (data == null || data.length == 0) {
      return ResponseEntity.notFound().build();
    }

    MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
    try {
      if (type != null && !type.isBlank()) mediaType = MediaType.parseMediaType(type);
    } catch (Exception ignored) {
    }

    return ResponseEntity.ok()
      .contentType(mediaType)
      .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
      .body(data);
  }

  @GetMapping("/{id}/reviews")
  public List<ReviewDto> reviews(@PathVariable Long id) {
    return reviewRepository.findByArticleIdOrderByCreatedAtDesc(id).stream().map(ReviewDto::from).toList();
  }

  @PostMapping("/{id}/reviews")
  public ReviewDto createReview(@PathVariable Long id, @RequestBody @Valid ReviewCreateReq req, Authentication authentication) {
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

  private static String variationImageUrl(VariationArticle v, int index) {
    byte[] data = switch (index) {
      case 1 -> v.getImageData1();
      case 2 -> v.getImageData2();
      case 3 -> v.getImageData3();
      case 4 -> v.getImageData4();
      default -> null;
    };
    return data != null && data.length > 0 ? "/api/articles/variation-image/" + v.getId() + "/" + index : null;
  }

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
    private String imageUrl;
    private String imageUrl2;
    private String imageUrl3;
    private String imageUrl4;

    static VariationDto from(VariationArticle v) {
      VariationDto d = new VariationDto();
      d.id = v.getId();
      d.quantiteStock = v.getQuantiteStock();
      d.prix = v.getPrix();
      d.couleurId = v.getCouleur().getId();
      d.couleurNom = v.getCouleur().getNom();
      d.couleurCodeHex = v.getCouleur().getCodeHex();
      d.tailleId = v.getTaille().getId();
      d.taillePointure = v.getTaille().getPointure();
      d.imageUrl = variationImageUrl(v, 1);
      d.imageUrl2 = variationImageUrl(v, 2);
      d.imageUrl3 = variationImageUrl(v, 3);
      d.imageUrl4 = variationImageUrl(v, 4);
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

    static ColorDto from(List<VariationArticle> sameColor) {
      VariationArticle first = sameColor.get(0);
      ColorDto d = new ColorDto();
      d.couleurId = first.getCouleur().getId();
      d.couleurNom = first.getCouleur().getNom();
      d.couleurCodeHex = first.getCouleur().getCodeHex();
      d.totalStock = sameColor.stream().mapToInt(VariationArticle::getQuantiteStock).sum();
      d.sizes = sameColor.stream().map(v -> v.getTaille().getPointure()).distinct().toList();
      d.previewImage = variationImageUrl(first, 1);
      return d;
    }
  }

  private static List<ColorDto> colorDtos(List<VariationArticle> vars) {
    Map<Long, List<VariationArticle>> grouped = new LinkedHashMap<>();
    for (VariationArticle v : vars) {
      grouped.computeIfAbsent(v.getCouleur().getId(), k -> new java.util.ArrayList<>()).add(v);
    }
    return grouped.values().stream().map(ColorDto::from).toList();
  }
}