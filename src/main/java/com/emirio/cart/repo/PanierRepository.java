package com.emirio.cart.repo;

import com.emirio.cart.Panier;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PanierRepository extends JpaRepository<Panier, Long> {

    @EntityGraph(attributePaths = {"lignes"})
    Optional<Panier> findByClientId(Long clientId);
}