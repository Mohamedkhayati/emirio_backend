package com.emirio.cart.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.emirio.cart.Panier;

import java.util.Optional;

public interface PanierRepository extends JpaRepository<Panier, Long> {
    Optional<Panier> findByClientId(Long clientId);
}