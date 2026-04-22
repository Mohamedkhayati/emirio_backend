package com.emirio.reclamation.repository;

import com.emirio.reclamation.entity.ReclamationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReclamationMessageRepository extends JpaRepository<ReclamationMessage, Long> {
}