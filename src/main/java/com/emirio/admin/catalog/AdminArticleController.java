package com.emirio.admin.catalog;

import com.emirio.catalog.Article;
import com.emirio.catalog.Category;
import com.emirio.catalog.repo.ArticleRepository;
import com.emirio.catalog.repo.CategoryRepository;
import lombok.Data;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/admin/articles")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AdminArticleController {

  private final ArticleRepository articles;
  private final CategoryRepository categories;

  public AdminArticleController(ArticleRepository articles, CategoryRepository categories) {
    this.articles = articles;
    this.categories = categories;
  }

  @GetMapping
  public List<ArticleDto> list() {
    return articles.findAllByOrderByIdDesc().stream().map(ArticleDto::from).toList();
  }

  @GetMapping("/{id}")
  public ArticleDto details(@PathVariable Long id) {
    return ArticleDto.from(articles.findById(id).orElseThrow());
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ArticleDto create(
    @RequestPart("data") CreateReq req,
    @RequestPart(value = "image1", required = false) MultipartFile image1,
    @RequestPart(value = "image2", required = false) MultipartFile image2,
    @RequestPart(value = "image3", required = false) MultipartFile image3,
    @RequestPart(value = "image4", required = false) MultipartFile image4
  ) throws IOException {
    Category cat = categories.findById(req.getCategorieId()).orElseThrow();
    Article a = new Article();
    fill(a, req, cat, image1, image2, image3, image4);
    return ArticleDto.from(articles.save(a));
  }

  @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ArticleDto update(
    @PathVariable Long id,
    @RequestPart("data") CreateReq req,
    @RequestPart(value = "image1", required = false) MultipartFile image1,
    @RequestPart(value = "image2", required = false) MultipartFile image2,
    @RequestPart(value = "image3", required = false) MultipartFile image3,
    @RequestPart(value = "image4", required = false) MultipartFile image4
  ) throws IOException {
    Category cat = categories.findById(req.getCategorieId()).orElseThrow();
    Article a = articles.findById(id).orElseThrow();
    fill(a, req, cat, image1, image2, image3, image4);
    return ArticleDto.from(articles.save(a));
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    articles.deleteById(id);
  }

  private void fill(
    Article a,
    CreateReq req,
    Category cat,
    MultipartFile image1,
    MultipartFile image2,
    MultipartFile image3,
    MultipartFile image4
  ) throws IOException {
    a.setNom(req.getNom());
    a.setDescription(req.getDescription());
    a.setDetails(req.getDetails());
    a.setPrix(req.getPrix());
    a.setActif(req.isActif());
    a.setCategorie(cat);
    a.setMarque(req.getMarque());
    a.setMatiere(req.getMatiere());
    a.setSku(req.getSku());
    a.setRecommended(req.isRecommended());
    a.setSalePrice(req.getSalePrice());
    a.setSaleStartAt(req.getSaleStartAt());
    a.setSaleEndAt(req.getSaleEndAt());

    if (image1 != null && !image1.isEmpty()) {
      a.setImageData1(image1.getBytes());
      a.setImageName1(image1.getOriginalFilename());
      a.setImageType1(image1.getContentType());
    }
    if (image2 != null && !image2.isEmpty()) {
      a.setImageData2(image2.getBytes());
      a.setImageName2(image2.getOriginalFilename());
      a.setImageType2(image2.getContentType());
    }
    if (image3 != null && !image3.isEmpty()) {
      a.setImageData3(image3.getBytes());
      a.setImageName3(image3.getOriginalFilename());
      a.setImageType3(image3.getContentType());
    }
    if (image4 != null && !image4.isEmpty()) {
      a.setImageData4(image4.getBytes());
      a.setImageName4(image4.getOriginalFilename());
      a.setImageType4(image4.getContentType());
    }
  }

  @Data
  public static class CreateReq {
    private String nom;
    private String description;
    private String details;
    private double prix;
    private boolean actif;
    private Long categorieId;
    private String marque;
    private String matiere;
    private String sku;
    private Double salePrice;
    private java.time.LocalDateTime saleStartAt;
    private java.time.LocalDateTime saleEndAt;
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
    private java.time.LocalDateTime saleStartAt;
    private java.time.LocalDateTime saleEndAt;
    private boolean recommended;

    static ArticleDto from(Article a) {
      ArticleDto d = new ArticleDto();
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
      d.imageUrl = a.getImageData1() != null ? "/api/articles/" + a.getId() + "/image/1" : null;
      d.salePrice = a.getSalePrice();
      d.saleStartAt = a.getSaleStartAt();
      d.saleEndAt = a.getSaleEndAt();
      d.recommended = a.isRecommended();
      return d;
    }
  }
}
