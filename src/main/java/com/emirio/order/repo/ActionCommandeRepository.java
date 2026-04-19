package com.emirio.order.repo;

import com.emirio.order.ActionCommande;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActionCommandeRepository extends JpaRepository<ActionCommande, Long> {
    List<ActionCommande> findByCommandeIdOrderByDateActionDesc(Long commandeId);
}