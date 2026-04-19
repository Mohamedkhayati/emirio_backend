package com.emirio.favorite;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping
    public ResponseEntity<List<Long>> getUserFavorites() {
        return ResponseEntity.ok(favoriteService.getUserFavoriteIds());
    }

    @PostMapping("/{articleId}")
    public ResponseEntity<Void> addFavorite(@PathVariable Long articleId) {
        favoriteService.addFavorite(articleId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{articleId}")
    public ResponseEntity<Void> removeFavorite(@PathVariable Long articleId) {
        favoriteService.removeFavorite(articleId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{articleId}/check")
    public ResponseEntity<Boolean> checkFavorite(@PathVariable Long articleId) {
        return ResponseEntity.ok(favoriteService.isFavorite(articleId));
    }
}