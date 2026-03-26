package com.emirio.cart.repo;

import com.emirio.cart.LignePanier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LignePanierRepository extends JpaRepository<LignePanier, Long> {
    Optional<LignePanier> findByPanierIdAndVariationId(Long panierId, Long variationId);
}