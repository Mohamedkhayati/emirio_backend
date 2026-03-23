package com.emirio.admin.catalog;

import com.emirio.catalog.Article;
import com.emirio.catalog.Color;
import com.emirio.catalog.Size;
import com.emirio.catalog.VariationArticle;
import com.emirio.catalog.repo.ArticleRepository;
import com.emirio.catalog.repo.ColorRepository;
import com.emirio.catalog.repo.SizeRepository;
import com.emirio.catalog.repo.VariationRepository;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminVariationController {
  private final VariationRepository variations;
  private final ArticleRepository articles;
  private final ColorRepository colors;
  private final SizeRepository sizes;

  public AdminVariationController(
    VariationRepository variations,
    ArticleRepository articles,
    ColorRepository colors,
    SizeRepository sizes
  ) {
    this.variations = variations;
    this.articles = articles;
    this.colors = colors;
    this.sizes = sizes;
  }

  @GetMapping("/articles/{articleId}/variations")
  public List<VariationDto> listForArticle(@PathVariable Long articleId) {
    return variations.findByArticleId(articleId).stream().map(VariationDto::from).toList();
  }

  @PostMapping("/articles/{articleId}/variations")
  public VariationDto createForArticle(@PathVariable Long articleId, @RequestBody CreateReq req) {
    Article a = articles.findById(articleId).orElseThrow();
    Color c = colors.findById(req.getCouleurId()).orElseThrow();
    Size s = sizes.findById(req.getTailleId()).orElseThrow();

    VariationArticle v = new VariationArticle();
    v.setArticle(a);
    v.setCouleur(c);
    v.setTaille(s);
    v.setPrix(req.getPrix());
    v.setQuantiteStock(req.getQuantiteStock());

    return VariationDto.from(variations.save(v));
  }

  @PutMapping("/variations/{id}")
  public VariationDto update(@PathVariable Long id, @RequestBody CreateReq req) {
    VariationArticle v = variations.findById(id).orElseThrow();
    Color c = colors.findById(req.getCouleurId()).orElseThrow();
    Size s = sizes.findById(req.getTailleId()).orElseThrow();

    v.setCouleur(c);
    v.setTaille(s);
    v.setPrix(req.getPrix());
    v.setQuantiteStock(req.getQuantiteStock());

    return VariationDto.from(variations.save(v));
  }

  @DeleteMapping("/variations/{id}")
  public void delete(@PathVariable Long id) {
    variations.deleteById(id);
  }

  @Data
  public static class CreateReq {
    @PositiveOrZero private double prix;
    @PositiveOrZero private int quantiteStock;
    private Long couleurId;
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
      return d;
    }
  }
}
