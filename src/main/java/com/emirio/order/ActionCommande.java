package com.emirio.order;

import com.emirio.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "action_commande")
public class ActionCommande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_action", nullable = false, updatable = false)
    private LocalDateTime dateAction;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_action", nullable = false, length = 50)
    private TypeActionCommande typeAction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "commande_id", nullable = false)
    private Commande commande;

    // 🔽 THIS IS THE MAPPING YOU NEED
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User utilisateur;

    @Column(name = "ancien_statut", length = 50)
    private String ancienStatut;

    @Column(name = "nouveau_statut", length = 50)
    private String nouveauStatut;

    @Column(name = "details", length = 1000)
    private String details;

    @PrePersist
    void onCreate() {
        if (dateAction == null) {
            dateAction = LocalDateTime.now();
        }
    }
}