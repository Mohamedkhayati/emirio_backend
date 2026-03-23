package com.emirio.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "utilisateur")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String mdp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "date_de_creation", nullable = false, updatable = false)
    private Instant dateDeCreation;

    @Column(name = "statut_compte", nullable = false)
    private String statutCompte;

    @PrePersist
    protected void onCreate() {
        if (dateDeCreation == null) {
            dateDeCreation = Instant.now();
        }
        if (statutCompte == null || statutCompte.isBlank()) {
            statutCompte = "ACTIVE";
        }
        if (role == null) {
            role = Role.CLIENT;
        }
    }
}
