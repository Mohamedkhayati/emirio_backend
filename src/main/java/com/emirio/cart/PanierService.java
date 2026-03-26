package com.emirio.cart;

import com.emirio.cart.dto.CartItemRequest;
import com.emirio.cart.dto.PanierResponse;
import com.emirio.cart.dto.PanierSyncRequest;
import com.emirio.cart.repo.PanierRepository;
import com.emirio.user.User;
import com.emirio.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class PanierService {

    private final PanierRepository panierRepository;
    private final UserRepository userRepository;

    public PanierService(PanierRepository panierRepository, UserRepository userRepository) {
        this.panierRepository = panierRepository;
        this.userRepository = userRepository;
    }

    protected User currentUser(String email) {
        return userRepository.findByEmail(email).orElseThrow();
    }

    protected Panier getOrCreatePanier(User user) {
        return panierRepository.findByClientId(user.getId())
            .orElseGet(() -> {
                Panier panier = new Panier();
                panier.setClient(user);
                return panierRepository.save(panier);
            });
    }

    @Transactional
    public PanierResponse getMyPanier(String email) {
        User user = currentUser(email);
        Panier panier = getOrCreatePanier(user);
        return PanierResponse.from(panier);
    }

    @Transactional
    public PanierResponse sync(String email, PanierSyncRequest request) {
        User user = currentUser(email);
        Panier panier = getOrCreatePanier(user);

        panier.getLignes().clear();

        for (CartItemRequest item : request.getItems()) {
            if (item.getQty() <= 0) continue;

            LignePanier ligne = new LignePanier();
            ligne.setArticleId(item.getArticleId());
            ligne.setVariationId(item.getVariationId());
            ligne.setNomProduit(item.getNom());
            ligne.setImageUrl(item.getImageUrl());
            ligne.setCouleurNom(item.getCouleurNom());
            ligne.setTaillePointure(item.getTaillePointure());
            ligne.setQuantite(item.getQty());
            ligne.setPrixUnitaire(item.getPrix());

            panier.addLigne(ligne);
        }

        Panier saved = panierRepository.save(panier);
        return PanierResponse.from(saved);
    }

    @Transactional
    public void clear(String email) {
        User user = currentUser(email);
        Panier panier = getOrCreatePanier(user);
        panier.getLignes().clear();
        panierRepository.save(panier);
    }
}