package com.emirio.admin.catalog;

import com.emirio.catalog.Article;
import com.emirio.catalog.Color;
import com.emirio.catalog.Size;
import com.emirio.catalog.VariationArticle;
import com.emirio.catalog.repo.ArticleRepository;
import com.emirio.catalog.repo.ColorRepository;
import com.emirio.catalog.repo.SizeRepository;
import com.emirio.catalog.repo.VariationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AdminVariationController {

  private final VariationRepository variations;
  private final ArticleRepository articles;
  private final ColorRepository colors;
  private final SizeRepository sizes;
  private final ObjectMapper objectMapper;

  public AdminVariationController(
    VariationRepository variations,
    ArticleRepository articles,
    ColorRepository colors,
    SizeRepository sizes,
    ObjectMapper objectMapper
  ) {
    this.variations = variations;
    this.articles = articles;
    this.colors = colors;
    this.sizes = sizes;
    this.objectMapper = objectMapper;
  }

  @GetMapping("/articles/{articleId}/variations")
  public List<VariationDto> listForArticle(@PathVariable Long articleId) {
    ensureArticleExists(articleId);
    return variations.findByArticleIdOrderByIdAsc(articleId).stream().map(VariationDto::from).toList();
  }

  @PostMapping(value = "/articles/{articleId}/variations", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public VariationDto createForArticle(
    @PathVariable Long articleId,
    @RequestPart("data") @Valid CreateReq req,
    @RequestPart(value = "image1", required = false) MultipartFile image1,
    @RequestPart(value = "image2", required = false) MultipartFile image2,
    @RequestPart(value = "image3", required = false) MultipartFile image3,
    @RequestPart(value = "image4", required = false) MultipartFile image4,
    @RequestPart(value = "model3d", required = false) MultipartFile model3d
  ) throws IOException {
    Article article = findArticle(articleId);
    Color color = findColor(req.getCouleurId());
    Size size = findSize(req.getTailleId());

    if (variations.existsByArticleIdAndCouleurIdAndTailleId(articleId, req.getCouleurId(), req.getTailleId())) {
      throw new ResponseStatusException(BAD_REQUEST, "This variation already exists for the selected article");
    }

    VariationArticle v = new VariationArticle();
    v.setArticle(article);
    v.setCouleur(color);
    v.setTaille(size);
    v.setPrix(req.getPrix());
    v.setQuantiteStock(req.getQuantiteStock());

    applyImages(v, image1, image2, image3, image4);
    applyModel3d(v, model3d);

    return VariationDto.from(variations.save(v));
  }

  
  @PutMapping(value = "/variations/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public VariationDto update(
      @PathVariable Long id,
      @RequestPart("data") @Valid CreateReq req,
      @RequestPart(value = "image1", required = false) MultipartFile image1,
      @RequestPart(value = "image2", required = false) MultipartFile image2,
      @RequestPart(value = "image3", required = false) MultipartFile image3,
      @RequestPart(value = "image4", required = false) MultipartFile image4,
      @RequestPart(value = "model3d", required = false) MultipartFile model3d
  ) throws IOException {
      VariationArticle v = findVariation(id);
      Color color = findColor(req.getCouleurId());
      Size size = findSize(req.getTailleId());

      if (variations.existsByArticleIdAndCouleurIdAndTailleIdAndIdNot(
          v.getArticle().getId(),
          req.getCouleurId(),
          req.getTailleId(),
          id
      )) {
          throw new ResponseStatusException(BAD_REQUEST, "This variation already exists for the selected article");
      }

      v.setCouleur(color);
      v.setTaille(size);
      v.setPrix(req.getPrix());
      v.setQuantiteStock(req.getQuantiteStock());

      applyImages(v, image1, image2, image3, image4);
      applyModel3d(v, model3d);

      return VariationDto.from(variations.save(v));
  }
  

  @DeleteMapping("/variations/{id}")
  public void delete(@PathVariable Long id) {
    VariationArticle v = findVariation(id);
    variations.delete(v);
  }

  @GetMapping("/variations/{id}/image/{index}")
  public @ResponseBody byte[] image(@PathVariable Long id, @PathVariable int index) {
    VariationArticle v = findVariation(id);
    return switch (index) {
      case 1 -> imageOr404(v.getImageData1());
      case 2 -> imageOr404(v.getImageData2());
      case 3 -> imageOr404(v.getImageData3());
      case 4 -> imageOr404(v.getImageData4());
      default -> throw new ResponseStatusException(NOT_FOUND, "Image not found");
    };
  }

  private CreateReq readReq(String dataJson) {
    try {
      return objectMapper.readValue(dataJson, CreateReq.class);
    } catch (Exception e) {
      throw new ResponseStatusException(BAD_REQUEST, "Invalid variation data");
    }
  }

  private byte[] imageOr404(byte[] data) {
    if (data == null || data.length == 0) {
      throw new ResponseStatusException(NOT_FOUND, "Image not found");
    }
    return data;
  }

  private void applyImages(
    VariationArticle v,
    MultipartFile image1,
    MultipartFile image2,
    MultipartFile image3,
    MultipartFile image4
  ) throws IOException {
    applyImage(v, 1, image1);
    applyImage(v, 2, image2);
    applyImage(v, 3, image3);
    applyImage(v, 4, image4);
  }

  private void applyImage(VariationArticle v, int index, MultipartFile file) throws IOException {
    if (file == null || file.isEmpty()) return;

    if (index == 1) {
      v.setImageData1(file.getBytes());
      v.setImageName1(file.getOriginalFilename());
      v.setImageType1(file.getContentType());
    } else if (index == 2) {
      v.setImageData2(file.getBytes());
      v.setImageName2(file.getOriginalFilename());
      v.setImageType2(file.getContentType());
    } else if (index == 3) {
      v.setImageData3(file.getBytes());
      v.setImageName3(file.getOriginalFilename());
      v.setImageType3(file.getContentType());
    } else if (index == 4) {
      v.setImageData4(file.getBytes());
      v.setImageName4(file.getOriginalFilename());
      v.setImageType4(file.getContentType());
    }
  }

  private void applyModel3d(VariationArticle v, MultipartFile model3d) throws IOException {
    if (model3d == null || model3d.isEmpty()) return;

    v.setModel3dData(model3d.getBytes());
    v.setModel3dName(model3d.getOriginalFilename());

    String contentType = model3d.getContentType();
    if (contentType == null || contentType.isBlank()) {
      String name = model3d.getOriginalFilename() == null ? "" : model3d.getOriginalFilename().toLowerCase();
      if (name.endsWith(".glb")) {
        contentType = "model/gltf-binary";
      } else if (name.endsWith(".gltf")) {
        contentType = "model/gltf+json";
      } else {
        contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
      }
    }

    v.setModel3dType(contentType);
  }

  private void ensureArticleExists(Long articleId) {
    if (!articles.existsById(articleId)) {
      throw new ResponseStatusException(NOT_FOUND, "Article not found");
    }
  }

  private Article findArticle(Long id) {
    return articles.findById(id)
      .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Article not found"));
  }

  private Color findColor(Long id) {
    return colors.findById(id)
      .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Color not found"));
  }

  private Size findSize(Long id) {
    return sizes.findById(id)
      .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Size not found"));
  }

  private VariationArticle findVariation(Long id) {
    return variations.findById(id)
      .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Variation not found"));
  }

  private static String imageUrl(VariationArticle v, int index) {
    byte[] data = switch (index) {
      case 1 -> v.getImageData1();
      case 2 -> v.getImageData2();
      case 3 -> v.getImageData3();
      case 4 -> v.getImageData4();
      default -> null;
    };
    return data != null && data.length > 0 ? "/api/admin/variations/" + v.getId() + "/image/" + index : null;
  }

  private static String model3dUrl(VariationArticle v) {
    byte[] data = v.getModel3dData();
    return data != null && data.length > 0 ? "/api/articles/variation-model/" + v.getId() : null;
  }

  @Data
  public static class CreateReq {
    @Positive
    private double prix;

    @PositiveOrZero
    private int quantiteStock;

    @NotNull
    private Long couleurId;

    @NotNull
    private Long tailleId;
  }

  @Data
  public static class VariationDto {
    private Long id;
    private double prix;
    private int quantiteStock;
    private Long articleId;
    private Long couleurId;
    private String couleurNom;
    private String couleurCodeHex;
    private Long tailleId;
    private String taillePointure;
    private String imageUrl;
    private String imageUrl2;
    private String imageUrl3;
    private String imageUrl4;
    private String model3dUrl;

    static VariationDto from(VariationArticle v) {
      VariationDto d = new VariationDto();
      d.id = v.getId();
      d.prix = v.getPrix();
      d.quantiteStock = v.getQuantiteStock();
      d.articleId = v.getArticle().getId();
      d.couleurId = v.getCouleur().getId();
      d.couleurNom = v.getCouleur().getNom();
      d.couleurCodeHex = v.getCouleur().getCodeHex();
      d.tailleId = v.getTaille().getId();
      d.taillePointure = v.getTaille().getPointure();
      d.imageUrl = imageUrl(v, 1);
      d.imageUrl2 = imageUrl(v, 2);
      d.imageUrl3 = imageUrl(v, 3);
      d.imageUrl4 = imageUrl(v, 4);
      d.model3dUrl = model3dUrl(v);
      return d;
    }
  }
}