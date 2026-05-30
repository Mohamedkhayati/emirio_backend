package com.emirio.vendeur.controller;

import com.emirio.vendeur.dto.ArticleVendeurDto;
import com.emirio.vendeur.dto.VariationVendeurDto;
import com.emirio.vendeur.service.VendeurArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/vendeur/articles")

@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequiredArgsConstructor
public class VendeurArticleController {

    private final VendeurArticleService service;

    @GetMapping
    public List<ArticleVendeurDto> listMyArticles() {
        return service.getMyArticles();
    }

    @GetMapping("/{id}")
    public ArticleVendeurDto getArticle(@PathVariable Long id) {
        return service.getMyArticle(id);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ArticleVendeurDto createArticle(
            @RequestPart("data") VendeurArticleService.ArticleCreateRequest req,
            @RequestPart(value = "image1", required = false) MultipartFile image1,
            @RequestPart(value = "image2", required = false) MultipartFile image2,
            @RequestPart(value = "image3", required = false) MultipartFile image3,
            @RequestPart(value = "image4", required = false) MultipartFile image4) throws IOException {
        return service.createArticle(req, image1, image2, image3, image4);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ArticleVendeurDto updateArticle(
            @PathVariable Long id,
            @RequestPart("data") VendeurArticleService.ArticleUpdateRequest req,
            @RequestPart(value = "image1", required = false) MultipartFile image1,
            @RequestPart(value = "image2", required = false) MultipartFile image2,
            @RequestPart(value = "image3", required = false) MultipartFile image3,
            @RequestPart(value = "image4", required = false) MultipartFile image4) throws IOException {
        return service.updateArticle(id, req, image1, image2, image3, image4);
    }

    @DeleteMapping("/{id}")
    public void deleteArticle(@PathVariable Long id) {
        service.deleteArticle(id);
    }

    // Variations endpoints
    @GetMapping("/{articleId}/variations")
    public List<VariationVendeurDto> getVariations(@PathVariable Long articleId) {
        ArticleVendeurDto article = service.getMyArticle(articleId);
        return article.getVariations();
    }

    @PostMapping(value = "/{articleId}/variations", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<VariationVendeurDto> createVariations(
            @PathVariable Long articleId,
            @RequestPart("data") VendeurArticleService.VariationCreateRequest req,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestPart(value = "model3d", required = false) MultipartFile model3d) throws IOException {
        return service.createVariations(articleId, req, images, model3d);
    }

    @PutMapping(value = "/variations/{variationId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VariationVendeurDto updateVariation(
            @PathVariable Long variationId,
            @RequestPart("data") VendeurArticleService.VariationUpdateRequest req,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestPart(value = "model3d", required = false) MultipartFile model3d) throws IOException {
        return service.updateVariation(variationId, req, images, model3d);
    }

    @DeleteMapping("/variations/{variationId}")
    public void deleteVariation(@PathVariable Long variationId) {
        service.deleteVariation(variationId);
    }
}