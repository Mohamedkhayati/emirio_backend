// com.emirio.reclamation.repository.ReclamationHistoryRepository
package com.emirio.reclamation.repository;

import com.emirio.reclamation.entity.ReclamationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReclamationHistoryRepository extends JpaRepository<ReclamationHistory, Long> {
    List<ReclamationHistory> findByReclamationIdOrderByCreatedAtAsc(Long reclamationId);
}