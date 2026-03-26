package com.emirio.order;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "ligne_commande")
public class LigneCommande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long articleId;

    private Long variationId;

    @Column(nullable = false)
    private String nomProduit;

    private String imageUrl;

    private String couleurNom;

    private String taillePointure;

    @Column(nullable = false)
    private int quantite;

    @Column(nullable = false)
    private double prixUnitaire;

    @Column(nullable = false)
    private double sousTotal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "commande_id", nullable = false)
    private Commande commande;

    @PrePersist
    @PreUpdate
    void calculeSousTotal() {
        if (quantite < 1) quantite = 1;
        sousTotal = prixUnitaire * quantite;
    }
}