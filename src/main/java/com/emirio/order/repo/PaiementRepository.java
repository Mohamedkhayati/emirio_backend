package com.emirio.order.repo;

import com.emirio.order.Paiement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaiementRepository extends JpaRepository<Paiement, Long> {
    List<Paiement> findByCommandeIdOrderByDatePaiementDesc(Long commandeId);
}