package com.emirio.admin;

import com.emirio.order.Commande;
import com.emirio.order.StatutCommande;
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
        Commande commande = commandes.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Order not found"));

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

    @PatchMapping("/{id}/archive")
    @Transactional
    public ResponseEntity<?> archive(@PathVariable Long id) {
        Commande commande = commandes.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Order not found"));

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
        Commande commande = commandes.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Order not found"));

        commande.setArchived(false);
        Commande saved = commandes.save(commande);

        return ResponseEntity.ok(Map.of(
            "message", "Order restored successfully",
            "archived", false,
            "order", toDto(saved)
        ));
    }

    private AdminOrderDto toDto(Commande c) {
        AdminOrderDto d = new AdminOrderDto();
        d.setId(c.getId());
        d.setReferenceCommande(c.getReferenceCommande());
        d.setDateCommande(c.getDateCommande());
        d.setStatutCommande(c.getStatutCommande().name());
        d.setTotal(c.getTotal());
        d.setArchived(c.isArchived());
        d.setNomClient(c.getNomClient());
        d.setPrenomClient(c.getPrenomClient());
        d.setEmailClient(c.getEmailClient());
        d.setTelephone(c.getTelephone());
        d.setVille(c.getVille());
        d.setNombreLignes(c.getLignes() != null ? c.getLignes().size() : 0);
        return d;
    }

    @Data
    public static class UpdateStatusReq {
        @NotBlank
        private String statutCommande;
    }

    @Data
    public static class AdminOrderDto {
        private Long id;
        private String referenceCommande;
        private LocalDateTime dateCommande;
        private String statutCommande;
        private double total;
        private boolean archived;
        private String nomClient;
        private String prenomClient;
        private String emailClient;
        private String telephone;
        private String ville;
        private int nombreLignes;
    }
}