package com.emirio.cart.repo;

import com.emirio.cart.InteractionPanier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InteractionPanierRepository extends JpaRepository<InteractionPanier, Long> {
    List<InteractionPanier> findByPanierIdOrderByDateActionDesc(Long panierId);
}