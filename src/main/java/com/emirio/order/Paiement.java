package com.emirio.order;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "paiement")
@Getter
@Setter
@NoArgsConstructor
public class Paiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "commande_id", nullable = false)
    private Commande commande;

    @Column(nullable = false)
    private double montant;

    @Column(name = "date_paiement", nullable = false)
    private LocalDateTime datePaiement;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_paiement", nullable = false, length = 30)
    private ModePaiement modePaiement;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_paiement", nullable = false, length = 30)
    private StatutPaiement statutPaiement;

    @Column(name = "reference_transaction", length = 100)
    private String referenceTransaction;

    @Column(length = 1000)
    private String details;

    @PrePersist
    protected void onCreate() {
        if (datePaiement == null) {
            datePaiement = LocalDateTime.now();
        }
    }

    public Paiement(Commande commande, double montant, ModePaiement modePaiement,
                    StatutPaiement statutPaiement, String referenceTransaction, String details) {
        this.commande = commande;
        this.montant = montant;
        this.modePaiement = modePaiement;
        this.statutPaiement = statutPaiement;
        this.referenceTransaction = referenceTransaction;
        this.details = details;
        this.datePaiement = LocalDateTime.now();
    }
}