package com.emirio.recommendation.repository;

import com.emirio.recommendation.entity.UserRecommendation;
import com.emirio.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface UserRecommendationRepository extends JpaRepository<UserRecommendation, Long> {
    List<UserRecommendation> findByUserOrderByRankAsc(User user);

    @Modifying
    @Transactional
    @Query("DELETE FROM UserRecommendation ur WHERE ur.user = :user")
    void deleteByUser(User user);
}