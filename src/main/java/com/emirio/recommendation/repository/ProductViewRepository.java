package com.emirio.recommendation.repository;

import com.emirio.recommendation.entity.ProductView;
import com.emirio.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductViewRepository extends JpaRepository<ProductView, Long> {

    @EntityGraph(attributePaths = {"article", "article.categorie"})
    List<ProductView> findByUser(User user);
}