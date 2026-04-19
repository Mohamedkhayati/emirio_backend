package com.emirio.order;

import com.emirio.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.emirio.order.repo.ActionCommandeRepository;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutPaiement statutPaiement = StatutPaiement.EN_ATTENTE_VERIFICATION;

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
    @Column(length = 30)
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
    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Paiement> paiements = new ArrayList<>();

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
    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, orphanRemoval = false)
    @OrderBy("dateAction DESC")
    private List<ActionCommande> actions = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (dateCommande == null) {
            dateCommande = LocalDateTime.now();
        }
        if (statutCommande == null) {
            statutCommande = StatutCommande.EN_ATTENTE;
        }
        if (statutPaiement == null) {
            statutPaiement = StatutPaiement.EN_ATTENTE_VERIFICATION;
        }
        if (referenceCommande == null || referenceCommande.isBlank()) {
            referenceCommande = "CMD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }
    public void addPaiement(Paiement paiement) {
        paiements.add(paiement);
        paiement.setCommande(this);
    }

    public void addLigne(LigneCommande ligne) {
        if (ligne == null) return;
        ligne.setCommande(this);
        this.lignes.add(ligne);
    }

    public void clearLignes() {
        for (LigneCommande ligne : lignes) {
            ligne.setCommande(null);
        }
        lignes.clear();
    }
}