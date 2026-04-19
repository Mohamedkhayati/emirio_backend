package com.emirio.favorite;

import com.emirio.catalog.Article;
import com.emirio.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUser(User user);
    Optional<Favorite> findByUserAndArticle(User user, Article article);
    void deleteByUserAndArticle(User user, Article article);
    boolean existsByUserAndArticle(User user, Article article);
}