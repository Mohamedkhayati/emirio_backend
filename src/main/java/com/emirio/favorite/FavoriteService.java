package com.emirio.favorite;

import com.emirio.catalog.Article;
import com.emirio.catalog.repo.ArticleRepository;  // adjust import
import com.emirio.user.User;
import com.emirio.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;  // use UserRepository directly

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public void addFavorite(Long articleId) {
        User user = getCurrentUser();
        Article article = articleRepository.findById(articleId)
            .orElseThrow(() -> new RuntimeException("Article not found"));
        if (!favoriteRepository.existsByUserAndArticle(user, article)) {
            favoriteRepository.save(new Favorite(user, article));
        }
    }

    @Transactional
    public void removeFavorite(Long articleId) {
        User user = getCurrentUser();
        Article article = articleRepository.findById(articleId)
            .orElseThrow(() -> new RuntimeException("Article not found"));
        favoriteRepository.deleteByUserAndArticle(user, article);
    }

    public List<Long> getUserFavoriteIds() {
        User user = getCurrentUser();
        return favoriteRepository.findByUser(user).stream()
            .map(fav -> fav.getArticle().getId())
            .collect(Collectors.toList());
    }

    public boolean isFavorite(Long articleId) {
        User user = getCurrentUser();
        Article article = articleRepository.findById(articleId).orElse(null);
        return article != null && favoriteRepository.existsByUserAndArticle(user, article);
    }
}