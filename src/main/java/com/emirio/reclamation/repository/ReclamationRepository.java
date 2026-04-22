package com.emirio.reclamation.repository;

import com.emirio.reclamation.entity.Reclamation;
import com.emirio.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReclamationRepository extends JpaRepository<Reclamation, Long> {
    List<Reclamation> findByUserOrderByCreatedAtDesc(User user);
    
    @Query("SELECT r FROM Reclamation r LEFT JOIN FETCH r.messages WHERE r.id = :id")
    java.util.Optional<Reclamation> findByIdWithMessages(@Param("id") Long id);
    
    List<Reclamation> findAllByOrderByCreatedAtDesc();
}