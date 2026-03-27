package com.emirio.admin;

import com.emirio.order.Commande;
import com.emirio.order.LigneCommande;
import com.emirio.order.StatutCommande;
import com.emirio.order.StatutPaiement;
import com.emirio.order.repo.CommandeRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/admin/orders")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AdminOrderController {

    private final CommandeRepository commandes;
    private final OrderMailService orderMailService;

    public AdminOrderController(CommandeRepository commandes, OrderMailService orderMailService) {
        this.commandes = commandes;
        this.orderMailService = orderMailService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<AdminOrderDto> all() {
        return commandes.findAll().stream()
            .sorted((a, b) -> b.getDateCommande().compareTo(a.getDateCommande()))
            .map(this::toDto)
            .toList();
    }

    @PatchMapping("/{id}/status")
    @Transactional
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody UpdateStatusReq req) {
        Commande commande = requireOrder(id);

        StatutCommande newStatus = StatutCommande.valueOf(req.getStatutCommande().trim().toUpperCase());
        commande.setStatutCommande(newStatus);

        Commande saved = commandes.save(commande);

        if (newStatus == StatutCommande.CONFIRMEE) {
            orderMailService.sendConfirmedEmail(saved);
        } else if (newStatus == StatutCommande.ANNULEE) {
            orderMailService.sendCancelledEmail(saved);
        }

        return ResponseEntity.ok(Map.of(
            "message", "Order status updated successfully",
            "status", saved.getStatutCommande().name(),
            "order", toDto(saved)
        ));
    }

    @PatchMapping("/{id}/confirm")
    @Transactional
    public ResponseEntity<?> confirm(@PathVariable Long id) {
        UpdateStatusReq req = new UpdateStatusReq();
        req.setStatutCommande("CONFIRMEE");
        return updateStatus(id, req);
    }

    @PatchMapping("/{id}/cancel")
    @Transactional
    public ResponseEntity<?> cancel(@PathVariable Long id) {
        UpdateStatusReq req = new UpdateStatusReq();
        req.setStatutCommande("ANNULEE");
        return updateStatus(id, req);
    }

    @PatchMapping("/{id}/payment-review")
    @Transactional
    public ResponseEntity<?> reviewPayment(@PathVariable Long id, @RequestBody PaymentReviewReq req) {
        Commande commande = requireOrder(id);

        try {
            commande.setAdminDecisionNote(req.getNote());
        } catch (Exception ignored) {
        }

        if (req.isAccepted()) {
            try {
                commande.setStatutPaiement(StatutPaiement.ACCEPTE);
            } catch (Exception ignored) {
            }
            commande.setStatutCommande(StatutCommande.CONFIRMEE);
        } else {
            try {
                commande.setStatutPaiement(StatutPaiement.REFUSE);
            } catch (Exception ignored) {
            }
            commande.setStatutCommande(StatutCommande.ANNULEE);
        }

        Commande saved = commandes.save(commande);

        if (req.isAccepted()) {
            orderMailService.sendPaymentAcceptedEmail(saved);
        } else {
            orderMailService.sendPaymentRejectedEmail(saved);
        }

        return ResponseEntity.ok(Map.of(
            "message", "Payment reviewed successfully",
            "order", toDto(saved)
        ));
    }

    @PatchMapping("/{id}/delivered")
    @Transactional
    public ResponseEntity<?> delivered(@PathVariable Long id) {
        Commande commande = requireOrder(id);

        commande.setStatutCommande(StatutCommande.LIVREE);
        try {
            commande.setDeliveredAt(LocalDateTime.now());
        } catch (Exception ignored) {
        }

        Commande saved = commandes.save(commande);
        orderMailService.sendDeliveredEmail(saved);

        return ResponseEntity.ok(Map.of(
            "message", "Order marked as delivered",
            "order", toDto(saved)
        ));
    }

    @PatchMapping("/{id}/archive")
    @Transactional
    public ResponseEntity<?> archive(@PathVariable Long id) {
        Commande commande = requireOrder(id);

        commande.setArchived(true);
        Commande saved = commandes.save(commande);
        orderMailService.sendArchivedEmail(saved);

        return ResponseEntity.ok(Map.of(
            "message", "Order archived successfully",
            "archived", true,
            "order", toDto(saved)
        ));
    }

    @PatchMapping("/{id}/unarchive")
    @Transactional
    public ResponseEntity<?> unarchive(@PathVariable Long id) {
        Commande commande = requireOrder(id);

        commande.setArchived(false);
        Commande saved = commandes.save(commande);

        return ResponseEntity.ok(Map.of(
            "message", "Order restored successfully",
            "archived", false,
            "order", toDto(saved)
        ));
    }

    private Commande requireOrder(Long id) {
        return commandes.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Order not found"));
    }

    private AdminOrderDto toDto(Commande c) {
        AdminOrderDto d = new AdminOrderDto();
        d.setId(c.getId());
        d.setReferenceCommande(c.getReferenceCommande());
        d.setDateCommande(c.getDateCommande());
        d.setStatutCommande(c.getStatutCommande() != null ? c.getStatutCommande().name() : null);

        try {
            d.setStatutPaiement(c.getStatutPaiement() != null ? c.getStatutPaiement().name() : null);
        } catch (Exception ignored) {
        }

        d.setTotal(c.getTotal());
        d.setArchived(c.isArchived());
        d.setNomClient(c.getNomClient());
        d.setPrenomClient(c.getPrenomClient());
        d.setEmailClient(c.getEmailClient());
        d.setTelephone(c.getTelephone());

        try {
            d.setAdresse(c.getAdresse());
        } catch (Exception ignored) {
        }

        d.setVille(c.getVille());

        try {
            d.setCodePostal(c.getCodePostal());
        } catch (Exception ignored) {
        }

        try {
            d.setModePaiement(c.getModePaiement() != null ? c.getModePaiement().name() : null);
        } catch (Exception ignored) {
        }

        try {
            d.setCardLast4(c.getCardLast4());
        } catch (Exception ignored) {
        }

        try {
            d.setD17Phone(c.getD17Phone());
        } catch (Exception ignored) {
        }

        try {
            d.setD17Reference(c.getD17Reference());
        } catch (Exception ignored) {
        }

        try {
            d.setBankReference(c.getBankReference());
        } catch (Exception ignored) {
        }

        try {
            d.setPaymentInstructions(c.getPaymentInstructions());
        } catch (Exception ignored) {
        }

        try {
            d.setSignatureDataUrl(c.getSignatureDataUrl());
        } catch (Exception ignored) {
        }

        try {
            d.setInvoiceNumber(c.getInvoiceNumber());
        } catch (Exception ignored) {
        }

        try {
            d.setInvoiceUrl(c.getInvoiceUrl());
        } catch (Exception ignored) {
        }

        try {
            d.setAdminDecisionNote(c.getAdminDecisionNote());
        } catch (Exception ignored) {
        }

        try {
            d.setDeliveredAt(c.getDeliveredAt());
        } catch (Exception ignored) {
        }

        try {
            d.setNote(c.getNote());
        } catch (Exception ignored) {
        }

        List<AdminOrderLineDto> lignes = c.getLignes() == null
            ? List.of()
            : c.getLignes().stream().map(this::toLineDto).toList();
        d.setLignes(lignes);
        d.setNombreLignes(lignes.size());

        return d;
    }

    private AdminOrderLineDto toLineDto(LigneCommande l) {
        AdminOrderLineDto d = new AdminOrderLineDto();
        d.setId(l.getId());
        d.setArticleId(l.getArticleId());
        d.setVariationId(l.getVariationId());
        d.setNomProduit(l.getNomProduit());

        try {
            d.setImageUrl(l.getImageUrl());
        } catch (Exception ignored) {
        }

        try {
            d.setCouleurNom(l.getCouleurNom());
        } catch (Exception ignored) {
        }

        try {
            d.setTaillePointure(l.getTaillePointure());
        } catch (Exception ignored) {
        }

        d.setQuantite(l.getQuantite());
        d.setPrixUnitaire(l.getPrixUnitaire());
        d.setSousTotal(l.getSousTotal());
        return d;
    }

    @Data
    public static class UpdateStatusReq {
        @NotBlank
        private String statutCommande;
    }

    @Data
    public static class PaymentReviewReq {
        private boolean accepted;
        private String note;
    }

    @Data
    public static class AdminOrderDto {
        private Long id;
        private String referenceCommande;
        private LocalDateTime dateCommande;
        private String statutCommande;
        private String statutPaiement;
        private double total;
        private boolean archived;
        private String nomClient;
        private String prenomClient;
        private String emailClient;
        private String telephone;
        private String adresse;
        private String ville;
        private String codePostal;
        private String modePaiement;
        private String cardLast4;
        private String d17Phone;
        private String d17Reference;
        private String bankReference;
        private String paymentInstructions;
        private String signatureDataUrl;
        private String invoiceNumber;
        private String invoiceUrl;
        private String adminDecisionNote;
        private LocalDateTime deliveredAt;
        private String note;
        private int nombreLignes;
        private List<AdminOrderLineDto> lignes;
    }

    @Data
    public static class AdminOrderLineDto {
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