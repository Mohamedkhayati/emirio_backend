package com.emirio.recommendation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface UserArticleEventRepository extends JpaRepository<UserArticleEvent, Long> {

    @Query("""
        select e.categoryId, count(e.id)
        from UserArticleEvent e
        where e.userId = :userId and e.eventType = :eventType and e.categoryId is not null
        group by e.categoryId
        order by count(e.id) desc
    """)
    List<Object[]> findTopCategories(Long userId, String eventType);

    @Query("""
        select e.articleId, count(e.id)
        from UserArticleEvent e
        where e.userId = :userId and e.eventType = :eventType and e.articleId is not null
        group by e.articleId
        order by count(e.id) desc
    """)
    List<Object[]> findTopArticles(Long userId, String eventType);
}