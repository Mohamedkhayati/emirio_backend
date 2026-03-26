package com.emirio.cart;

import com.emirio.cart.repo.PanierRepository;
import com.emirio.user.User;
import com.emirio.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class CartController {

    private final PanierRepository paniers;
    private final UserRepository users;

    public CartController(PanierRepository paniers, UserRepository users) {
        this.paniers = paniers;
        this.users = users;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public CartDto getMyCart(Authentication authentication) {
        User user = currentUser(authentication);
        Panier panier = ensurePanier(user);
        return toDto(panier);
    }

    @PutMapping("/sync")
    @Transactional
    public CartDto syncCart(@Valid @RequestBody SyncCartReq req, Authentication authentication) {
        User user = currentUser(authentication);
        Panier panier = ensurePanier(user);

        panier.getLignes().clear();

        for (CartItemReq item : req.getItems()) {
            if (item.getQuantite() <= 0) continue;

            LignePanier ligne = new LignePanier();
            ligne.setPanier(panier);
            ligne.setArticleId(item.getArticleId());
            ligne.setVariationId(item.getVariationId());
            ligne.setNomProduit(item.getNomProduit());
            ligne.setImageUrl(item.getImageUrl());
            ligne.setCouleurNom(item.getCouleurNom());
            ligne.setTaillePointure(item.getTaillePointure());
            ligne.setQuantite(item.getQuantite());
            ligne.setPrixUnitaire(item.getPrixUnitaire());
            ligne.setSousTotal(item.getPrixUnitaire() * item.getQuantite());

            panier.getLignes().add(ligne);
        }

        panier.setDateMaj(LocalDateTime.now());
        Panier saved = paniers.save(panier);
        return toDto(saved);
    }

    @DeleteMapping
    @Transactional
    public void clear(Authentication authentication) {
        User user = currentUser(authentication);
        Panier panier = ensurePanier(user);
        panier.getLignes().clear();
        panier.setDateMaj(LocalDateTime.now());
        paniers.save(panier);
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Unauthorized");
        }
        return users.findByEmail(authentication.getName())
            .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Unauthorized"));
    }

    private Panier ensurePanier(User user) {
        return paniers.findByClientId(user.getId()).orElseGet(() -> {
            Panier p = new Panier();
            p.setClient(user);
            p.setDateCreation(LocalDateTime.now());
            p.setDateMaj(LocalDateTime.now());
            return paniers.save(p);
        });
    }

    private CartDto toDto(Panier panier) {
        CartDto dto = new CartDto();
        dto.setId(panier.getId());
        dto.setDateCreation(panier.getDateCreation());
        dto.setDateMaj(panier.getDateMaj());

        List<CartLineDto> items = panier.getLignes().stream().map(l -> {
            CartLineDto d = new CartLineDto();
            d.setId(l.getId());
            d.setArticleId(l.getArticleId());
            d.setVariationId(l.getVariationId());
            d.setNomProduit(l.getNomProduit());
            d.setImageUrl(l.getImageUrl());
            d.setCouleurNom(l.getCouleurNom());
            d.setTaillePointure(l.getTaillePointure());
            d.setQuantite(l.getQuantite());
            d.setPrixUnitaire(l.getPrixUnitaire());
            d.setSousTotal(l.getSousTotal());
            return d;
        }).toList();

        dto.setItems(items);
        dto.setTotalItems(items.stream().mapToInt(CartLineDto::getQuantite).sum());
        dto.setTotalAmount(items.stream().mapToDouble(CartLineDto::getSousTotal).sum());
        return dto;
    }

    @Data
    public static class SyncCartReq {
        @Valid
        private List<CartItemReq> items = new ArrayList<>();
    }

    @Data
    public static class CartItemReq {
        private Long articleId;
        private Long variationId;

        @NotBlank
        private String nomProduit;

        private String imageUrl;
        private String couleurNom;
        private String taillePointure;

        @Min(1)
        private int quantite;

        @NotNull
        private Double prixUnitaire;
    }

    @Data
    public static class CartDto {
        private Long id;
        private LocalDateTime dateCreation;
        private LocalDateTime dateMaj;
        private int totalItems;
        private double totalAmount;
        private List<CartLineDto> items;
    }

    @Data
    public static class CartLineDto {
        private Long id;
        private Long articleId;
        private Long variationId;
        private String nomProduit;
        private String imageUrl;
        private String couleurNom;
        private String taillePointure;
        private int quantite;
        private double prixUnitaire;
        private double sousTotal;
    }
}