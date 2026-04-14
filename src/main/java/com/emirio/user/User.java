package com.emirio.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

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

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Enumerated(EnumType.STRING)
    @Column(name = "sexe")
    private Gender sexe;

    @Column(name = "profile_completed", nullable = false)
    private boolean profileCompleted;

    @Column(name = "photo_name")
    private String photoName;

    @Column(name = "photo_type")
    private String photoType;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "photo_data", columnDefinition = "LONGBLOB")
    private byte[] photoData;

    @PrePersist
    protected void onCreate() {
        if (dateDeCreation == null) dateDeCreation = Instant.now();
        if (statutCompte == null || statutCompte.isBlank()) statutCompte = "ACTIVE";
        if (role == null) role = Role.USER;
    }
}