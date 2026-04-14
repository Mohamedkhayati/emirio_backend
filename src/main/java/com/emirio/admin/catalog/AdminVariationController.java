package com.emirio.admin.catalog;

import com.emirio.catalog.Article;
import com.emirio.catalog.Color;
import com.emirio.catalog.Size;
import com.emirio.catalog.VariationArticle;
import com.emirio.catalog.VariationImage;
import com.emirio.catalog.repo.ArticleRepository;
import com.emirio.catalog.repo.ColorRepository;
import com.emirio.catalog.repo.SizeRepository;
import com.emirio.catalog.repo.VariationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AdminVariationController {

    private final ArticleRepository articles;
    private final VariationRepository variations;
    private final ColorRepository colors;
    private final SizeRepository sizes;
    private final ObjectMapper objectMapper;

    public AdminVariationController(
        ArticleRepository articles,
        VariationRepository variations,
        ColorRepository colors,
        SizeRepository sizes,
        ObjectMapper objectMapper
    ) {
        this.articles = articles;
        this.variations = variations;
        this.colors = colors;
        this.sizes = sizes;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/articles/{articleId}/variations")
    public List<VariationArticle> listByArticle(@PathVariable Long articleId) {
        Article article = articles.findById(articleId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Article not found"));

        return variations.findForApiByArticleId(article.getId());
    }

    @PostMapping(value = "/articles/{articleId}/variations", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public List<VariationArticle> createForArticle(
        @PathVariable Long articleId,
        @RequestPart("data") String data,
        @RequestPart(value = "images", required = false) List<MultipartFile> images,
        @RequestPart(value = "model3d", required = false) MultipartFile model3d
    ) throws IOException {
        VariationCreateReq req = objectMapper.readValue(data, VariationCreateReq.class);

        Article article = articles.findById(articleId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Article not found"));

        Color color = colors.findById(req.getCouleurId())
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Color not found"));

        boolean accessory = isAccessory(article);
        List<VariationArticle> saved = new ArrayList<>();

        if (accessory) {
            if (req.getQuantiteStock() == null || req.getQuantiteStock() < 0) {
                throw new ResponseStatusException(BAD_REQUEST, "Accessory stock is required");
            }

            VariationArticle v = new VariationArticle();
            v.setArticle(article);
            v.setCouleur(color);
            v.setTaille(null);
            v.setPrix(req.getPrix());
            v.setQuantiteStock(req.getQuantiteStock());

            attachFiles(v, images, model3d);
            saved.add(variations.save(v));
            return saved;
        }

        if (req.getSizes() == null || req.getSizes().isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "At least one size is required");
        }

        for (SizeStockReq s : req.getSizes()) {
            Size size = sizes.findById(s.getTailleId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Size not found"));

            VariationArticle v = new VariationArticle();
            v.setArticle(article);
            v.setCouleur(color);
            v.setTaille(size);
            v.setPrix(req.getPrix());
            v.setQuantiteStock(s.getQuantiteStock());

            attachFiles(v, images, model3d);
            saved.add(variations.save(v));
        }

        return saved;
    }

    @PutMapping(value = "/variations/{variationId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public VariationArticle updateVariation(
        @PathVariable Long variationId,
        @RequestPart("data") String data,
        @RequestPart(value = "images", required = false) List<MultipartFile> images,
        @RequestPart(value = "model3d", required = false) MultipartFile model3d
    ) throws IOException {
        VariationUpdateReq req = objectMapper.readValue(data, VariationUpdateReq.class);

        VariationArticle v = variations.findById(variationId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Variation not found"));

        boolean accessory = isAccessory(v.getArticle());

        Color color = colors.findById(req.getCouleurId())
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Color not found"));

        v.setCouleur(color);
        v.setPrix(req.getPrix());
        v.setQuantiteStock(req.getQuantiteStock());

        if (accessory) {
            v.setTaille(null);
        } else {
            if (req.getTailleId() == null) {
                throw new ResponseStatusException(BAD_REQUEST, "Size is required");
            }

            Size size = sizes.findById(req.getTailleId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Size not found"));

            v.setTaille(size);
        }

        attachFiles(v, images, model3d);
        return variations.save(v);
    }

    @DeleteMapping("/variations/{variationId}")
    @Transactional
    public void deleteVariation(@PathVariable Long variationId) {
        VariationArticle v = variations.findById(variationId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Variation not found"));
        variations.delete(v);
    }

    private void attachFiles(
        VariationArticle variation,
        List<MultipartFile> images,
        MultipartFile model3d
    ) throws IOException {
        if (images != null && !images.isEmpty()) {
            variation.getImages().clear();

            for (MultipartFile file : images) {
                if (file == null || file.isEmpty()) continue;

                VariationImage img = new VariationImage();
                img.setVariation(variation);
                img.setImageData(file.getBytes());
                img.setImageType(file.getContentType());
                variation.getImages().add(img);
            }
        }

        if (model3d != null && !model3d.isEmpty()) {
            variation.setModel3dData(model3d.getBytes());
            variation.setModel3dName(model3d.getOriginalFilename());
            variation.setModel3dType(model3d.getContentType());
        }
    }

    private boolean isAccessory(Article article) {
        if (article == null || article.getCategorie() == null || article.getCategorie().getNom() == null) {
            return false;
        }

        String n = article.getCategorie().getNom().trim().toLowerCase();
        return n.equals("accessoire") || n.equals("accessory") || n.equals("accessories");
    }

    @Data
    public static class VariationCreateReq {
        @NotNull
        private Double prix;

        @NotNull
        private Long couleurId;

        private Integer quantiteStock;
        private List<SizeStockReq> sizes;
    }

    @Data
    public static class VariationUpdateReq {
        @NotNull
        private Double prix;

        @NotNull
        private Long couleurId;

        private Long tailleId;

        @NotNull
        @Min(0)
        private Integer quantiteStock;
    }

    @Data
    public static class SizeStockReq {
        @NotNull
        private Long tailleId;

        @NotNull
        @Min(0)
        private Integer quantiteStock;
    }
}