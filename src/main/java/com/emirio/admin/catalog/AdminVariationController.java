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

import org.springframework.transaction.annotation.Transactional; 

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api")
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

    @GetMapping("/admin/articles/{articleId}/variations")
    @Transactional(readOnly = true) 
    public List<VariationDto> listByArticle(@PathVariable Long articleId) {
        Article article = articles.findById(articleId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Article not found"));

        return variations.findForApiByArticleId(article.getId()).stream()
            .map(VariationDto::from)
            .toList();
    }

    @PostMapping(value = "/admin/articles/{articleId}/variations", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public List<VariationDto> createForArticle(
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

            return saved.stream().map(VariationDto::from).toList();
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

        return saved.stream().map(VariationDto::from).toList();
    }

    @PutMapping(value = "/admin/variations/{variationId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public VariationDto updateVariation(
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
        VariationArticle saved = variations.save(v);

        return VariationDto.from(saved);
    }

    @DeleteMapping("/admin/variations/{variationId}")
    @Transactional
    public void deleteVariation(@PathVariable Long variationId) {
        VariationArticle v = variations.findById(variationId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Variation not found"));
        variations.delete(v);
    }

    @GetMapping("/catalog/variations/{variationId}/images/{imageId}")
    @Transactional(readOnly = true) 
    public ResponseEntity<byte[]> getVariationImage(
        @PathVariable Long variationId,
        @PathVariable Long imageId
    ) {
        VariationArticle variation = variations.findById(variationId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Variation not found"));

        VariationImage found = null;
        if (variation.getImages() != null) {
            for (VariationImage img : variation.getImages()) {
                if (img.getId() != null && img.getId().equals(imageId)) {
                    found = img;
                    break;
                }
            }
        }

        if (found == null || found.getImageData() == null || found.getImageData().length == 0) {
            throw new ResponseStatusException(NOT_FOUND, "Image not found");
        }

        MediaType mediaType = parseMediaType(found.getImageType(), MediaType.IMAGE_JPEG);

        return ResponseEntity.ok()
            .contentType(mediaType)
            .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
            .body(found.getImageData());
    }

    @GetMapping("/catalog/variations/{variationId}/model")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> getVariationModel(@PathVariable Long variationId) {
        VariationArticle variation = variations.findById(variationId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Variation not found"));

        if (variation.getModel3dData() == null || variation.getModel3dData().length == 0) {
            throw new ResponseStatusException(NOT_FOUND, "Model not found");
        }

        MediaType mediaType = parseMediaType(
            variation.getModel3dType(),
            MediaType.APPLICATION_OCTET_STREAM
        );

        return ResponseEntity.ok()
            .contentType(mediaType)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"" + safeFileName(variation.getModel3dName(), "model.glb") + "\""
            )
            .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
            .body(variation.getModel3dData());
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
        return n.equals("accessoire") || 
               n.equals("accessory") || 
               n.equals("accessories") ||
               n.equals("sac a main") ||
               n.equals("sac à main") ||
               n.equals("pochette de soirée");
    }

    private MediaType parseMediaType(String raw, MediaType fallback) {
        try {
            if (raw == null || raw.isBlank()) return fallback;
            return MediaType.parseMediaType(raw);
        } catch (Exception e) {
            return fallback;
        }
    }

    private String safeFileName(String raw, String fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        return raw.replaceAll("[\\r\\n\\\\/\\t]", "_");
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

    @Data
    public static class VariationDto {
        private Long id;
        private Long articleId;
        private Long couleurId;
        private String couleurNom;
        private String couleurCodeHex;
        private Long tailleId;
        private String taillePointure;
        private Double prix;
        private Integer quantiteStock;
        private String model3dName;
        private String model3dType;
        private String model3dUrl;
        private List<String> imageUrls;

        public static VariationDto from(VariationArticle v) {
            VariationDto dto = new VariationDto();
            dto.setId(v.getId());
            dto.setArticleId(v.getArticle() != null ? v.getArticle().getId() : null);

            if (v.getCouleur() != null) {
                dto.setCouleurId(v.getCouleur().getId());
                dto.setCouleurNom(v.getCouleur().getNom());
                dto.setCouleurCodeHex(v.getCouleur().getCodeHex());
            }

            if (v.getTaille() != null) {
                dto.setTailleId(v.getTaille().getId());
                dto.setTaillePointure(v.getTaille().getPointure());
            }

            dto.setPrix(v.getPrix());
            dto.setQuantiteStock(v.getQuantiteStock());
            dto.setModel3dName(v.getModel3dName());
            dto.setModel3dType(v.getModel3dType());

            if (v.getModel3dData() != null && v.getModel3dData().length > 0) {
                dto.setModel3dUrl("/api/catalog/variations/" + v.getId() + "/model");
            } else {
                dto.setModel3dUrl(null);
            }

            List<String> urls = new ArrayList<>();
            if (v.getImages() != null && !v.getImages().isEmpty()) {
                for (VariationImage img : v.getImages()) {
                    if (img.getId() != null) {
                        urls.add("/api/catalog/variations/" + v.getId() + "/images/" + img.getId());
                    }
                }
            }
            dto.setImageUrls(urls);

            return dto;
        }
    }
}