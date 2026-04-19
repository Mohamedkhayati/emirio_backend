package com.emirio.cart;

import com.emirio.catalog.VariationArticle;
import com.emirio.catalog.repo.VariationRepository;
import com.emirio.cart.dto.CartItemRequest;
import com.emirio.cart.dto.PanierResponse;
import com.emirio.cart.dto.PanierSyncRequest;
import com.emirio.cart.repo.InteractionPanierRepository;
import com.emirio.cart.repo.PanierRepository;
import com.emirio.user.User;
import com.emirio.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class PanierService {

    private final PanierRepository panierRepository;
    private final UserRepository userRepository;
    private final VariationRepository variationRepository;
    private final InteractionPanierRepository interactionPanierRepository;

    public PanierService(
        PanierRepository panierRepository,
        UserRepository userRepository,
        VariationRepository variationRepository,
        InteractionPanierRepository interactionPanierRepository
    ) {
        this.panierRepository = panierRepository;
        this.userRepository = userRepository;
        this.variationRepository = variationRepository;
        this.interactionPanierRepository = interactionPanierRepository;
    }

    protected User currentUser(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(UNAUTHORIZED, "Unauthorized");
        }

        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Unauthorized"));
    }

    @Transactional
    protected Panier getOrCreatePanier(User user) {
        return panierRepository.findByClientId(user.getId())
            .orElseGet(() -> {
                Panier panier = new Panier();
                panier.setClient(user);
                return panierRepository.saveAndFlush(panier);
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

        List<CartItemRequest> incomingItems =
            request != null && request.getItems() != null ? request.getItems() : List.of();

        Map<Long, LignePanier> existingByVariationId = new LinkedHashMap<>();
        for (LignePanier ligne : panier.getLignes()) {
            existingByVariationId.put(ligne.getVariationId(), ligne);
        }

        Map<Long, CartItemRequest> requestedByVariationId = mergeRequests(incomingItems);

        for (Map.Entry<Long, CartItemRequest> entry : requestedByVariationId.entrySet()) {
            Long variationId = entry.getKey();
            CartItemRequest req = entry.getValue();

            LignePanier existing = existingByVariationId.get(variationId);
            int oldQty = existing != null ? existing.getQuantite() : 0;
            int newQty = Math.max(1, req.getQuantite());
            int delta = newQty - oldQty;

            if (delta > 0) {
                decreaseStock(variationId, delta);
            }
        }

        for (Map.Entry<Long, LignePanier> entry : existingByVariationId.entrySet()) {
            Long variationId = entry.getKey();
            LignePanier existing = entry.getValue();

            CartItemRequest req = requestedByVariationId.get(variationId);
            int newQty = req != null ? Math.max(1, req.getQuantite()) : 0;
            int oldQty = existing.getQuantite();
            int delta = newQty - oldQty;

            if (delta < 0) {
                increaseStock(variationId, -delta);
            }
        }

        for (Map.Entry<Long, LignePanier> entry : existingByVariationId.entrySet()) {
            Long variationId = entry.getKey();
            LignePanier existing = entry.getValue();

            if (!requestedByVariationId.containsKey(variationId)) {
                logInteraction(
                    panier,
                    user,
                    TypeActionPanier.SUPPRESSION_ARTICLE,
                    existing.getVariationId(),
                    existing.getArticleId(),
                    existing.getQuantite(),
                    0,
                    "Article supprimé du panier"
                );
            }
        }

        panier.getLignes().removeIf(ligne -> !requestedByVariationId.containsKey(ligne.getVariationId()));

        for (CartItemRequest req : requestedByVariationId.values()) {
            LignePanier ligne = existingByVariationId.get(req.getVariationId());
            boolean isNew = (ligne == null);
            int oldQty = isNew ? 0 : ligne.getQuantite();
            int newQty = Math.max(1, req.getQuantite());

            if (ligne == null) {
                ligne = new LignePanier();
                ligne.setPanier(panier);
                panier.addLigne(ligne);
            }

            ligne.setArticleId(req.getArticleId());
            ligne.setVariationId(req.getVariationId());
            ligne.setNomProduit(req.getNomProduit());
            ligne.setImageUrl(req.getImageUrl());
            ligne.setCouleurNom(req.getCouleurNom());
            ligne.setTaillePointure(req.getTaillePointure());
            ligne.setQuantite(newQty);
            ligne.setPrixUnitaire(req.getPrixUnitaire());

            if (isNew) {
                logInteraction(
                    panier,
                    user,
                    TypeActionPanier.AJOUT_ARTICLE,
                    req.getVariationId(),
                    req.getArticleId(),
                    0,
                    newQty,
                    "Article ajouté au panier"
                );
            } else if (oldQty != newQty) {
                logInteraction(
                    panier,
                    user,
                    TypeActionPanier.MODIFICATION_QUANTITE,
                    req.getVariationId(),
                    req.getArticleId(),
                    oldQty,
                    newQty,
                    "Quantité modifiée"
                );
            }
        }

        logInteraction(
            panier,
            user,
            TypeActionPanier.SYNCHRONISATION_PANIER,
            null,
            null,
            null,
            null,
            "Synchronisation complète du panier"
        );

        panier.touch();
        Panier saved = panierRepository.saveAndFlush(panier);
        return PanierResponse.from(saved);
    }

    @Transactional
    public void clear(String email) {
        User user = currentUser(email);
        Panier panier = getOrCreatePanier(user);

        for (LignePanier ligne : panier.getLignes()) {
            increaseStock(ligne.getVariationId(), ligne.getQuantite());

            logInteraction(
                panier,
                user,
                TypeActionPanier.SUPPRESSION_ARTICLE,
                ligne.getVariationId(),
                ligne.getArticleId(),
                ligne.getQuantite(),
                0,
                "Article supprimé lors du vidage panier"
            );
        }

        panier.clearLignes();

        logInteraction(
            panier,
            user,
            TypeActionPanier.VIDAGE_PANIER,
            null,
            null,
            null,
            null,
            "Panier vidé"
        );

        panier.touch();
        panierRepository.saveAndFlush(panier);
    }

    private void logInteraction(
        Panier panier,
        User user,
        TypeActionPanier typeAction,
        Long variationId,
        Long articleId,
        Integer ancienneQuantite,
        Integer nouvelleQuantite,
        String details
    ) {
        InteractionPanier interaction = new InteractionPanier();
        interaction.setPanier(panier);
        interaction.setUtilisateur(user);
        interaction.setTypeAction(typeAction);
        interaction.setVariationId(variationId);
        interaction.setArticleId(articleId);
        interaction.setAncienneQuantite(ancienneQuantite);
        interaction.setNouvelleQuantite(nouvelleQuantite);
        interaction.setDetails(details);
        interactionPanierRepository.save(interaction);
    }

    private Map<Long, CartItemRequest> mergeRequests(List<CartItemRequest> items) {
        Map<Long, CartItemRequest> map = new LinkedHashMap<>();

        for (CartItemRequest item : items) {
            if (item == null || item.getVariationId() == null || item.getArticleId() == null) {
                continue;
            }

            int qty = Math.max(1, item.getQuantite());

            if (!map.containsKey(item.getVariationId())) {
                CartItemRequest copy = new CartItemRequest();
                copy.setArticleId(item.getArticleId());
                copy.setVariationId(item.getVariationId());
                copy.setNomProduit(item.getNomProduit());
                copy.setImageUrl(item.getImageUrl());
                copy.setCouleurNom(item.getCouleurNom());
                copy.setTaillePointure(item.getTaillePointure());
                copy.setQuantite(qty);
                copy.setPrixUnitaire(item.getPrixUnitaire());
                map.put(copy.getVariationId(), copy);
            } else {
                CartItemRequest existing = map.get(item.getVariationId());
                existing.setQuantite(existing.getQuantite() + qty);
            }
        }

        return map;
    }

    private void decreaseStock(Long variationId, int qty) {
        if (qty <= 0) return;

        VariationArticle variation = variationRepository.findById(variationId)
            .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Variation not found: " + variationId));

        Integer stockValue = variation.getQuantiteStock();
        int stock = stockValue == null ? 0 : stockValue;

        if (stock < qty) {
            throw new ResponseStatusException(BAD_REQUEST, "Stock insuffisant pour la variation " + variationId);
        }

        variation.setQuantiteStock(stock - qty);
        variationRepository.save(variation);
    }

    private void increaseStock(Long variationId, int qty) {
        if (qty <= 0) return;

        VariationArticle variation = variationRepository.findById(variationId)
            .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Variation not found: " + variationId));

        Integer stockValue = variation.getQuantiteStock();
        int stock = stockValue == null ? 0 : stockValue;

        variation.setQuantiteStock(stock + qty);
        variationRepository.save(variation);
    }
}