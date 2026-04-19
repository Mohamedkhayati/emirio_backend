package com.emirio.cart;

import com.emirio.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
    name = "panier",
    uniqueConstraints = @UniqueConstraint(columnNames = "client_id")
)
public class Panier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(nullable = false)
    private LocalDateTime dateMaj;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false, unique = true)
    private User client;

    @OneToMany(mappedBy = "panier", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<LignePanier> lignes = new ArrayList<>();
    @OneToMany(mappedBy = "panier", cascade = CascadeType.ALL, orphanRemoval = false)
    @OrderBy("dateAction DESC")
    private List<InteractionPanier> interactions = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (dateCreation == null) dateCreation = LocalDateTime.now();
        if (dateMaj == null) dateMaj = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        dateMaj = LocalDateTime.now();
    }

    public void touch() {
        this.dateMaj = LocalDateTime.now();
    }

    public void addLigne(LignePanier ligne) {
        if (ligne == null) return;
        ligne.setPanier(this);
        this.lignes.add(ligne);
    }

    public void clearLignes() {
        this.lignes.clear();
    }

    public double getTotalPanier() {
        return lignes.stream().mapToDouble(LignePanier::getSousTotal).sum();
    }

    public int getTotalItems() {
        return lignes.stream().mapToInt(LignePanier::getQuantite).sum();
    }
}