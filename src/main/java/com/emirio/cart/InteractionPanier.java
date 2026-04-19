package com.emirio.cart;

import com.emirio.user.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "interaction_panier")
public class InteractionPanier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_action", nullable = false, updatable = false)
    private LocalDateTime dateAction;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_action", nullable = false, length = 50)
    private TypeActionPanier typeAction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "panier_id", nullable = false)
    private Panier panier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User utilisateur;

    @Column(name = "variation_id")
    private Long variationId;

    @Column(name = "article_id")
    private Long articleId;

    @Column(name = "ancienne_quantite")
    private Integer ancienneQuantite;

    @Column(name = "nouvelle_quantite")
    private Integer nouvelleQuantite;

    @Column(name = "details", length = 500)
    private String details;

    public InteractionPanier() {
    }

    @PrePersist
    public void onCreate() {
        if (dateAction == null) {
            dateAction = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getDateAction() {
        return dateAction;
    }

    public void setDateAction(LocalDateTime dateAction) {
        this.dateAction = dateAction;
    }

    public TypeActionPanier getTypeAction() {
        return typeAction;
    }

    public void setTypeAction(TypeActionPanier typeAction) {
        this.typeAction = typeAction;
    }

    public Panier getPanier() {
        return panier;
    }

    public void setPanier(Panier panier) {
        this.panier = panier;
    }

    public User getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(User utilisateur) {
        this.utilisateur = utilisateur;
    }

    public Long getVariationId() {
        return variationId;
    }

    public void setVariationId(Long variationId) {
        this.variationId = variationId;
    }

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public Integer getAncienneQuantite() {
        return ancienneQuantite;
    }

    public void setAncienneQuantite(Integer ancienneQuantite) {
        this.ancienneQuantite = ancienneQuantite;
    }

    public Integer getNouvelleQuantite() {
        return nouvelleQuantite;
    }

    public void setNouvelleQuantite(Integer nouvelleQuantite) {
        this.nouvelleQuantite = nouvelleQuantite;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}