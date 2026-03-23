package com.emirio.catalog.api;

import com.emirio.catalog.Article;
import com.emirio.catalog.ArticleReview;
import com.emirio.catalog.VariationArticle;
import com.emirio.catalog.repo.ArticleRepository;
import com.emirio.catalog.repo.ArticleReviewRepository;
import com.emirio.catalog.repo.VariationRepository;
import com.emirio.user.User;
import com.emirio.user.UserRepository;
import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

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
    List<Article> list = (categorieId == null)
      ? articles.findByActifTrueOrderByIdDesc()
      : articles.findByCategorieIdAndActifTrueOrderByIdDesc(categorieId);

    return list.stream().map(a -> {
      List<VariationArticle> vars = variations.findByArticleId(a.getId());
      return ArticleListDto.from(a, vars);
    }).toList();
  }

  @GetMapping("/{id}")
  public ArticleDetailsDto details(@PathVariable Long id) {
    Article a = articles.findById(id).orElseThrow();
    List<VariationArticle> vars = variations.findByArticleId(id);
    return ArticleDetailsDto.from(a, vars);
  }

  @GetMapping("/{id}/image/{index}")
  public ResponseEntity<byte[]> image(@PathVariable Long id, @PathVariable int index) {
    Article a = articles.findById(id).orElseThrow();

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
    } catch (Exception ignored) {}

    return ResponseEntity.ok()
      .contentType(mediaType)
      .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
      .body(data);
  }

  @GetMapping("/{id}/reviews")
  public List<ReviewDto> reviews(@PathVariable Long id) {
    return reviewRepository.findByArticleIdOrderByCreatedAtDesc(id)
      .stream()
      .map(ReviewDto::from)
      .toList();
  }

  @PostMapping("/{id}/reviews")
  public ReviewDto createReview(@PathVariable Long id, @RequestBody ReviewCreateReq req, Authentication authentication) {
    if (authentication == null || authentication.getName() == null) {
      throw new RuntimeException("Unauthorized");
    }

    Article article = articles.findById(id).orElseThrow();
    User user = userRepository.findByEmail(authentication.getName()).orElseThrow();

    ArticleReview review = new ArticleReview();
    review.setArticle(article);
    review.setUser(user);
    review.setRating(Math.max(1, Math.min(5, req.getRating())));
    review.setComment(req.getComment());

    return ReviewDto.from(reviewRepository.save(review));
  }

  @Data
  public static class ReviewCreateReq {
    private int rating;
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

//only the DTO parts to add/change inside your existing PublicArticleController

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
 private java.time.LocalDateTime saleStartAt;
 private java.time.LocalDateTime saleEndAt;
 private boolean recommended;
 private List<VariationDto> variations;

 static ArticleListDto from(Article a, List<VariationArticle> vars) {
   ArticleListDto d = new ArticleListDto();
   d.id = a.getId();
   d.nom = a.getNom();
   d.description = a.getDescription();
   d.prix = a.getPrix();
   d.actif = a.isActif();
   d.categorieId = a.getCategorie().getId();
   d.categorieNom = a.getCategorie().getNom();
   d.marque = a.getMarque();
   d.matiere = a.getMatiere();
   d.sku = a.getSku();
   d.imageUrl = a.getImageData1() != null ? "/api/articles/" + a.getId() + "/image/1" : null;
   d.imageUrl2 = a.getImageData2() != null ? "/api/articles/" + a.getId() + "/image/2" : null;
   d.imageUrl3 = a.getImageData3() != null ? "/api/articles/" + a.getId() + "/image/3" : null;
   d.imageUrl4 = a.getImageData4() != null ? "/api/articles/" + a.getId() + "/image/4" : null;
   d.salePrice = a.getSalePrice();
   d.saleStartAt = a.getSaleStartAt();
   d.saleEndAt = a.getSaleEndAt();
   d.recommended = a.isRecommended();
   d.variations = vars.stream().map(VariationDto::from).toList();
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
    private List<VariationDto> variations;

    static ArticleDetailsDto from(Article a, List<VariationArticle> vars) {
      ArticleDetailsDto d = new ArticleDetailsDto();
      d.id = a.getId();
      d.nom = a.getNom();
      d.description = a.getDescription();
      d.details = a.getDetails();
      d.prix = a.getPrix();
      d.actif = a.isActif();
      d.categorieId = a.getCategorie().getId();
      d.categorieNom = a.getCategorie().getNom();
      d.marque = a.getMarque();
      d.matiere = a.getMatiere();
      d.sku = a.getSku();
      d.imageUrl = a.getImageData1() != null ? "/api/articles/" + a.getId() + "/image/1" : null;
      d.imageUrl2 = a.getImageData2() != null ? "/api/articles/" + a.getId() + "/image/2" : null;
      d.imageUrl3 = a.getImageData3() != null ? "/api/articles/" + a.getId() + "/image/3" : null;
      d.imageUrl4 = a.getImageData4() != null ? "/api/articles/" + a.getId() + "/image/4" : null;
      d.variations = vars.stream().map(VariationDto::from).toList();
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
      return d;
    }
  }
}
