package com.emirio.admin;

import com.emirio.order.*;
import com.emirio.order.repo.ActionCommandeRepository;
import com.emirio.order.repo.CommandeRepository;
import com.emirio.order.repo.PaiementRepository;
import com.emirio.user.User;
import com.emirio.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/admin/orders")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AdminOrderController {

    private static final Logger log = LoggerFactory.getLogger(AdminOrderController.class);

    private final CommandeRepository commandes;
    private final OrderMailService orderMailService;
    private final PaiementRepository paiementRepository;
    private final ActionCommandeRepository actionCommandeRepository;
    private final UserRepository userRepository;

    public AdminOrderController(CommandeRepository commandes,
                                OrderMailService orderMailService,
                                PaiementRepository paiementRepository,
                                ActionCommandeRepository actionCommandeRepository,
                                UserRepository userRepository) {
        this.commandes = commandes;
        this.orderMailService = orderMailService;
        this.paiementRepository = paiementRepository;
        this.actionCommandeRepository = actionCommandeRepository;
        this.userRepository = userRepository;
    }

    // Helper to get the currently authenticated admin user
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "User not found"));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<AdminOrderDto> all() {
        return commandes.findAll().stream()
                .sorted((a, b) -> b.getDateCommande().compareTo(a.getDateCommande()))
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/{id}/actions")
    public ResponseEntity<?> getOrderActions(@PathVariable Long id) {
        requireOrder(id);
        List<ActionCommande> actions = actionCommandeRepository.findByCommandeIdOrderByDateActionDesc(id);
        List<Map<String, Object>> result = actions.stream().map(a -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", a.getId());
            map.put("typeAction", a.getTypeAction() != null ? a.getTypeAction().name() : null);
            map.put("dateAction", a.getDateAction());
            map.put("ancienStatut", a.getAncienStatut());
            map.put("nouveauStatut", a.getNouveauStatut());
            map.put("details", a.getDetails());
            String userName = null;
            if (a.getUtilisateur() != null) {
                userName = (a.getUtilisateur().getPrenom() != null ? a.getUtilisateur().getPrenom() : "")
                        + " " + (a.getUtilisateur().getNom() != null ? a.getUtilisateur().getNom() : "");
                userName = userName.trim();
            }
            map.put("utilisateurNom", userName);
            return map;
        }).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/payments")
    public ResponseEntity<?> getPayments(@PathVariable Long id) {
        requireOrder(id);
        List<Paiement> payments = paiementRepository.findByCommandeIdOrderByDatePaiementDesc(id);
        List<Map<String, Object>> result = payments.stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("montant", p.getMontant());
            map.put("datePaiement", p.getDatePaiement());
            map.put("modePaiement", p.getModePaiement() != null ? p.getModePaiement().name() : null);
            map.put("statutPaiement", p.getStatutPaiement() != null ? p.getStatutPaiement().name() : null);
            map.put("referenceTransaction", p.getReferenceTransaction());
            map.put("details", p.getDetails());
            return map;
        }).toList();
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/status")
    @Transactional
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusReq req) {
        User admin = getCurrentUser();
        Commande commande = requireOrder(id);
        StatutCommande oldStatus = commande.getStatutCommande();
        StatutCommande newStatus = StatutCommande.valueOf(req.getStatutCommande().trim().toUpperCase());
        commande.setStatutCommande(newStatus);
        Commande saved = commandes.save(commande);
        logAction(commande, admin, TypeActionCommande.CHANGEMENT_STATUT,
                oldStatus != null ? oldStatus.name() : null, newStatus.name(),
                "Admin changed status from " + oldStatus + " to " + newStatus);
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
    public ResponseEntity<?> reviewPayment(@PathVariable Long id, @Valid @RequestBody PaymentReviewReq req) {
        User admin = getCurrentUser();
        Commande commande = requireOrder(id);
        StatutPaiement oldPaymentStatus = commande.getStatutPaiement();
        StatutCommande oldOrderStatus = commande.getStatutCommande();

        commande.setAdminDecisionNote(req.getNote());
        if (req.isAccepted()) {
            commande.setStatutPaiement(StatutPaiement.ACCEPTE);
            commande.setStatutCommande(StatutCommande.CONFIRMEE);

            // Create a Paiement record for traceability
            Paiement paiement = new Paiement();
            paiement.setCommande(commande);
            paiement.setMontant(commande.getTotal());
            paiement.setModePaiement(commande.getModePaiement());
            paiement.setStatutPaiement(StatutPaiement.ACCEPTE);
            paiement.setReferenceTransaction("ADMIN_ACCEPT_" + System.currentTimeMillis());
            paiement.setDetails("Payment accepted by admin. Note: " + (req.getNote() != null ? req.getNote() : ""));
            paiementRepository.save(paiement);
        } else {
            commande.setStatutPaiement(StatutPaiement.REFUSE);
            commande.setStatutCommande(StatutCommande.ANNULEE);
        }

        Commande saved = commandes.save(commande);
        logAction(commande, admin, TypeActionCommande.CHANGEMENT_STATUT_PAIEMENT,
                oldPaymentStatus != null ? oldPaymentStatus.name() : null,
                saved.getStatutPaiement().name(),
                "Payment review by admin. Accepted=" + req.isAccepted() + ". Note: " + (req.getNote() != null ? req.getNote() : ""));

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
        User admin = getCurrentUser();
        Commande commande = requireOrder(id);
        StatutCommande oldStatus = commande.getStatutCommande();
        commande.setStatutCommande(StatutCommande.LIVREE);
        commande.setDeliveredAt(LocalDateTime.now());
        Commande saved = commandes.save(commande);
        logAction(commande, admin, TypeActionCommande.CHANGEMENT_STATUT,
                oldStatus != null ? oldStatus.name() : null,
                StatutCommande.LIVREE.name(),
                "Order marked as delivered by admin");
        orderMailService.sendDeliveredEmail(saved);
        return ResponseEntity.ok(Map.of(
                "message", "Order marked as delivered",
                "order", toDto(saved)
        ));
    }

    @PatchMapping("/{id}/archive")
    @Transactional
    public ResponseEntity<?> archive(@PathVariable Long id) {
        User admin = getCurrentUser();
        Commande commande = requireOrder(id);
        commande.setArchived(true);
        Commande saved = commandes.save(commande);
        logAction(commande, admin, TypeActionCommande.ARCHIVAGE_COMMANDE,
                null, null, "Order archived by admin");
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
        User admin = getCurrentUser();
        Commande commande = requireOrder(id);
        commande.setArchived(false);
        Commande saved = commandes.save(commande);
        logAction(commande, admin, TypeActionCommande.ARCHIVAGE_COMMANDE,
                null, null, "Order restored from archive by admin");
        return ResponseEntity.ok(Map.of(
                "message", "Order restored successfully",
                "archived", false,
                "order", toDto(saved)
        ));
    }

    // ------------------ PRIVATE HELPERS ------------------
    private void logAction(Commande commande, User adminUser, TypeActionCommande type,
                           String ancien, String nouveau, String details) {
        ActionCommande action = new ActionCommande();
        action.setCommande(commande);
        action.setUtilisateur(adminUser);   // CRITICAL: set the admin user
        action.setTypeAction(type);
        action.setAncienStatut(ancien);
        action.setNouveauStatut(nouveau);
        action.setDetails(details);
        actionCommandeRepository.save(action);
    }

    private Commande requireOrder(Long id) {
        return commandes.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Order not found"));
    }

    // ------------------ DTO MAPPING ------------------
    private AdminOrderDto toDto(Commande c) {
        AdminOrderDto d = new AdminOrderDto();
        d.setId(c.getId());
        d.setReferenceCommande(c.getReferenceCommande());
        d.setDateCommande(c.getDateCommande());
        d.setStatutCommande(c.getStatutCommande() != null ? c.getStatutCommande().name() : null);
        d.setStatutPaiement(c.getStatutPaiement() != null ? c.getStatutPaiement().name() : null);
        d.setTotal(c.getTotal());
        d.setArchived(c.isArchived());
        d.setNomClient(c.getNomClient());
        d.setPrenomClient(c.getPrenomClient());
        d.setEmailClient(c.getEmailClient());
        d.setTelephone(c.getTelephone());
        d.setAdresse(c.getAdresse());
        d.setVille(c.getVille());
        d.setCodePostal(c.getCodePostal());
        d.setModePaiement(c.getModePaiement() != null ? c.getModePaiement().name() : null);
        d.setCardLast4(c.getCardLast4());
        d.setD17Phone(c.getD17Phone());
        d.setD17Reference(c.getD17Reference());
        d.setBankReference(c.getBankReference());
        d.setPaymentInstructions(c.getPaymentInstructions());
        d.setSignatureDataUrl(c.getSignatureDataUrl());
        d.setInvoiceNumber(c.getInvoiceNumber());
        d.setInvoiceUrl(c.getInvoiceUrl());
        d.setAdminDecisionNote(c.getAdminDecisionNote());
        d.setDeliveredAt(c.getDeliveredAt());
        d.setNote(c.getNote());
        List<AdminOrderLineDto> lignes = c.getLignes() == null ? List.of() : c.getLignes().stream().map(this::toLineDto).toList();
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
        d.setImageUrl(l.getImageUrl());
        d.setCouleurNom(l.getCouleurNom());
        d.setTaillePointure(l.getTaillePointure());
        d.setQuantite(l.getQuantite());
        d.setPrixUnitaire(l.getPrixUnitaire());
        d.setSousTotal(l.getSousTotal());
        return d;
    }

    // ------------------ INNER DTOs ------------------
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