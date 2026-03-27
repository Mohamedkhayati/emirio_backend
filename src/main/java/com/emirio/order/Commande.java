package com.emirio.order;

import com.emirio.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "commande")
public class Commande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String referenceCommande;

    @Column(nullable = false)
    private LocalDateTime dateCommande;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutCommande statutCommande;

    @Column(nullable = false)
    private double total;

    @Column(nullable = false)
    private String nomClient;

    @Column(nullable = false)
    private String prenomClient;

    @Column(nullable = false)
    private String emailClient;

    @Column(nullable = false)
    private String telephone;

    @Column(nullable = false)
    private String adresse;

    @Column(nullable = false)
    private String ville;

    private String codePostal;

    @Enumerated(EnumType.STRING)
    private ModePaiement modePaiement;


    @Column(length = 1000)
    private String note;

    @Column(nullable = false)
    private boolean archived = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneCommande> lignes = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (dateCommande == null) dateCommande = LocalDateTime.now();
        if (statutCommande == null) statutCommande = StatutCommande.EN_ATTENTE;
        if (referenceCommande == null || referenceCommande.isBlank()) {
            referenceCommande = "CMD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }

    public void addLigne(LigneCommande ligne) {
        ligne.setCommande(this);
        this.lignes.add(ligne);
    }
   

    @Enumerated(EnumType.STRING)
    private StatutPaiement statutPaiement = StatutPaiement.EN_ATTENTE_VERIFICATION;

    private String cardLast4;
    private String d17Phone;
    private String d17Reference;
    private String bankReference;

    @Column(length = 2000)
    private String paymentInstructions;

    @Column(length = 500000)
    private String signatureDataUrl;

    private LocalDateTime signedAt;
    private String invoiceNumber;
    private String invoiceUrl;
    private LocalDateTime deliveredAt;

    @Column(length = 1000)
    private String adminDecisionNote;
}