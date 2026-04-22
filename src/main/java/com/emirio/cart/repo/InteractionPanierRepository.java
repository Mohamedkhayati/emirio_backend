package com.emirio.cart.repo;

import com.emirio.cart.InteractionPanier;
import com.emirio.cart.TypeActionPanier;
import com.emirio.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InteractionPanierRepository extends JpaRepository<InteractionPanier, Long> {
    List<InteractionPanier> findByUtilisateurAndTypeAction(User utilisateur, TypeActionPanier typeAction);
}