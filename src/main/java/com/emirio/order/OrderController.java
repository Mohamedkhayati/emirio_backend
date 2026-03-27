package com.emirio.order;

import com.emirio.admin.OrderMailService;
import com.emirio.cart.LignePanier;
import com.emirio.cart.Panier;
import com.emirio.cart.repo.PanierRepository;
import com.emirio.order.repo.CommandeRepository;
import com.emirio.user.User;
import com.emirio.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class OrderController {

    private final CommandeRepository commandes;
    private final PanierRepository paniers;
    private final UserRepository users;
    private final OrderCheckoutService orderCheckoutService;
    private final OrderMailService orderMailService;

    public OrderController(
        CommandeRepository commandes,
        PanierRepository paniers,
        UserRepository users,
        OrderCheckoutService orderCheckoutService,
        OrderMailService orderMailService
    ) {
        this.commandes = commandes;
        this.paniers = paniers;
        this.users = users;
        this.orderCheckoutService = orderCheckoutService;
        this.orderMailService = orderMailService;
    }

    @PostMapping("/checkout")
    @Transactional
    public OrderDetailsDto checkout(@Valid @RequestBody CheckoutReq req, Authentication authentication) {
        User user = currentUser(authentication);

        Panier panier = paniers.findByClientId(user.getId())
            .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Cart is empty"));

        if (panier.getLignes().isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Cart is empty");
        }

        Commande commande = new Commande();
        commande.setClient(user);
        commande.setDateCommande(LocalDateTime.now());
        commande.setStatutCommande(StatutCommande.EN_ATTENTE);

        commande.setNomClient(isBlank(req.getNom()) ? user.getNom() : req.getNom());
        commande.setPrenomClient(isBlank(req.getPrenom()) ? user.getPrenom() : req.getPrenom());
        commande.setEmailClient(user.getEmail());
        commande.setTelephone(req.getTelephone());
        commande.setAdresse(req.getAdresse());
        commande.setVille(req.getVille());
        commande.setCodePostal(req.getCodePostal());
        commande.setModePaiement(parseModePaiement(req.getModePaiement()));
        commande.setCardLast4(req.getCardLast4());
        commande.setNote(req.getNote());
        commande.setArchived(false);

        double total = 0.0;

        for (LignePanier lignePanier : panier.getLignes()) {
            LigneCommande ligneCommande = new LigneCommande();
            ligneCommande.setCommande(commande);
            ligneCommande.setArticleId(lignePanier.getArticleId());
            ligneCommande.setVariationId(lignePanier.getVariationId());
            ligneCommande.setNomProduit(lignePanier.getNomProduit());
            ligneCommande.setImageUrl(lignePanier.getImageUrl());
            ligneCommande.setCouleurNom(lignePanier.getCouleurNom());
            ligneCommande.setTaillePointure(lignePanier.getTaillePointure());
            ligneCommande.setQuantite(lignePanier.getQuantite());
            ligneCommande.setPrixUnitaire(lignePanier.getPrixUnitaire());
            ligneCommande.setSousTotal(lignePanier.getSousTotal());

            total += ligneCommande.getSousTotal();
            commande.getLignes().add(ligneCommande);
        }

        commande.setTotal(total);

        Commande saved = commandes.save(commande);

        saved.setInvoiceNumber(buildInvoiceNumber(saved));
        saved.setInvoiceUrl(orderCheckoutService.generateInvoiceUrl(saved));
        saved.setPaymentInstructions(buildPaymentInstructions(saved));

        saved = commandes.save(saved);

        panier.getLignes().clear();
        panier.setDateMaj(LocalDateTime.now());
        paniers.save(panier);

        try {
            orderMailService.sendInvoiceEmail(saved);
        } catch (Exception ignored) {
        }

        return toDetailsDto(saved);
    }

    @GetMapping("/my")
    @Transactional(readOnly = true)
    public List<OrderSummaryDto> myOrders(
        Authentication authentication,
        @RequestParam(required = false) Boolean archived
    ) {
        User user = currentUser(authentication);

        return commandes.findByClientIdOrderByDateCommandeDesc(user.getId()).stream()
            .filter(c -> archived == null || c.isArchived() == archived)
            .map(this::toSummaryDto)
            .toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public OrderDetailsDto details(@PathVariable Long id, Authentication authentication) {
        User user = currentUser(authentication);
        Commande commande = commandes.findByIdAndClientId(id, user.getId())
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Order not found"));
        return toDetailsDto(commande);
    }

    @PatchMapping("/{id}/cancel")
    @Transactional
    public OrderDetailsDto cancel(@PathVariable Long id, Authentication authentication) {
        User user = currentUser(authentication);
        Commande commande = commandes.findByIdAndClientId(id, user.getId())
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Order not found"));

        if (commande.getStatutCommande() != StatutCommande.EN_ATTENTE) {
            throw new ResponseStatusException(BAD_REQUEST, "This order can no longer be cancelled");
        }

        commande.setStatutCommande(StatutCommande.ANNULEE);
        return toDetailsDto(commandes.save(commande));
    }

    @PatchMapping("/{id}/archive")
    @Transactional
    public OrderDetailsDto archive(@PathVariable Long id, Authentication authentication) {
        User user = currentUser(authentication);
        Commande commande = commandes.findByIdAndClientId(id, user.getId())
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Order not found"));

        commande.setArchived(true);
        return toDetailsDto(commandes.save(commande));
    }

    private boolean isBlank(String v) {
        return v == null || v.isBlank();
    }

    private String buildInvoiceNumber(Commande c) {
        if (!isBlank(c.getInvoiceNumber())) {
            return c.getInvoiceNumber();
        }
        if (!isBlank(c.getReferenceCommande())) {
            return c.getReferenceCommande();
        }
        return "CMD-" + c.getId();
    }

    private String buildPaymentInstructions(Commande c) {
        String mode = c.getModePaiement() == null ? "" : c.getModePaiement().name();

        if ("LIVRAISON".equalsIgnoreCase(mode)) {
            return "Paiement à la livraison.";
        }

        if ("CARTE".equalsIgnoreCase(mode)) {
            String last4 = c.getCardLast4() == null ? "" : c.getCardLast4();
            return "Paiement par carte" + (last4.isBlank() ? "." : " - carte ****" + last4 + ".");
        }

        if ("D17".equalsIgnoreCase(mode)) {
            return "Veuillez effectuer le paiement via D17 et conserver la référence de paiement.";
        }

        if ("VIREMENT".equalsIgnoreCase(mode)) {
            return "Veuillez effectuer le virement bancaire et conserver la référence du virement.";
        }

        return "Suivez les instructions de paiement envoyées par email.";
    }

    private ModePaiement parseModePaiement(String value) {
        if (value == null || value.isBlank()) {
            return ModePaiement.LIVRAISON;
        }
        return ModePaiement.valueOf(value.trim().toUpperCase());
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Unauthorized");
        }
        return users.findByEmail(authentication.getName())
            .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Unauthorized"));
    }

    private OrderSummaryDto toSummaryDto(Commande c) {
        OrderSummaryDto d = new OrderSummaryDto();
        d.setId(c.getId());
        d.setReferenceCommande(c.getReferenceCommande());
        d.setDateCommande(c.getDateCommande());
        d.setStatutCommande(c.getStatutCommande().name());
        d.setTotal(c.getTotal());
        d.setNombreLignes(c.getLignes().size());
        d.setArchived(c.isArchived());
        return d;
    }

    private OrderDetailsDto toDetailsDto(Commande c) {
        OrderDetailsDto d = new OrderDetailsDto();
        d.setId(c.getId());
        d.setReferenceCommande(c.getReferenceCommande());
        d.setDateCommande(c.getDateCommande());
        d.setStatutCommande(c.getStatutCommande().name());
        d.setStatutPaiement(c.getStatutPaiement() != null ? c.getStatutPaiement().name() : null);
        d.setTotal(c.getTotal());
        d.setArchived(c.isArchived());
        d.setCancelable(c.getStatutCommande() == StatutCommande.EN_ATTENTE);
        d.setNomClient(c.getNomClient());
        d.setPrenomClient(c.getPrenomClient());
        d.setEmailClient(c.getEmailClient());
        d.setTelephone(c.getTelephone());
        d.setAdresse(c.getAdresse());
        d.setVille(c.getVille());
        d.setCodePostal(c.getCodePostal());
        d.setModePaiement(c.getModePaiement() != null ? c.getModePaiement().name() : null);
        d.setNote(c.getNote());
        d.setInvoiceNumber(c.getInvoiceNumber());
        d.setInvoiceUrl(c.getInvoiceUrl());
        d.setPaymentInstructions(c.getPaymentInstructions());

        List<OrderLineDto> lines = c.getLignes().stream().map(l -> {
            OrderLineDto x = new OrderLineDto();
            x.setId(l.getId());
            x.setVariationId(l.getVariationId());
            x.setArticleId(l.getArticleId());
            x.setArticleNom(l.getNomProduit());
            x.setCouleurNom(l.getCouleurNom());
            x.setTaillePointure(l.getTaillePointure());
            x.setQuantite(l.getQuantite());
            x.setPrixUnitaire(l.getPrixUnitaire());
            x.setSousTotal(l.getSousTotal());
            x.setImageUrl(l.getImageUrl());
            return x;
        }).toList();

        d.setLignes(lines);
        return d;
    }

    @Data
    public static class CheckoutReq {
        private String nom;
        private String prenom;

        @NotBlank
        private String telephone;

        @NotBlank
        private String adresse;

        @NotBlank
        private String ville;

        private String codePostal;

        @NotBlank
        private String modePaiement;

        private String cardLast4;
        private String note;
    }

    @Data
    public static class OrderSummaryDto {
        private Long id;
        private String referenceCommande;
        private LocalDateTime dateCommande;
        private String statutCommande;
        private double total;
        private int nombreLignes;
        private boolean archived;
    }

    @Data
    public static class OrderDetailsDto {
        private Long id;
        private String referenceCommande;
        private LocalDateTime dateCommande;
        private String statutCommande;
        private String statutPaiement;
        private double total;
        private boolean archived;
        private boolean cancelable;
        private String nomClient;
        private String prenomClient;
        private String emailClient;
        private String telephone;
        private String adresse;
        private String ville;
        private String codePostal;
        private String modePaiement;
        private String note;
        private String invoiceNumber;
        private String invoiceUrl;
        private String paymentInstructions;
        private List<OrderLineDto> lignes;
    }

    @Data
    public static class OrderLineDto {
        private Long id;
        private Long variationId;
        private Long articleId;
        private String articleNom;
        private String couleurNom;
        private String taillePointure;
        private int quantite;
        private double prixUnitaire;
        private double sousTotal;
        private String imageUrl;
    }
}