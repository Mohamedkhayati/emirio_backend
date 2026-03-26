package com.emirio.order.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.emirio.order.Commande;

import java.util.List;
import java.util.Optional;

public interface CommandeRepository extends JpaRepository<Commande, Long> {
    List<Commande> findByClientIdOrderByDateCommandeDesc(Long clientId);
    List<Commande> findByClientIdAndArchivedOrderByDateCommandeDesc(Long clientId, boolean archived);
    Optional<Commande> findByIdAndClientId(Long id, Long clientId);
    List<Commande> findAllByOrderByDateCommandeDesc();
}