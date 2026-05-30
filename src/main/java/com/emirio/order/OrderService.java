package com.emirio.order;

import com.emirio.admin.OrderMailService;
import com.emirio.catalog.VariationArticle;
import com.emirio.catalog.repo.VariationRepository;
import com.emirio.cart.LignePanier;
import com.emirio.cart.Panier;
import com.emirio.cart.repo.PanierRepository;
import com.emirio.order.dto.CheckoutRequest;
import com.emirio.order.dto.OrderDetailsDto;
import com.emirio.order.dto.OrderLineDto;
import com.emirio.order.dto.OrderSummaryDto;
import com.emirio.order.repo.ActionCommandeRepository;
import com.emirio.order.repo.CommandeRepository;
import com.emirio.order.repo.PaiementRepository;
import com.emirio.user.User;
import com.emirio.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final CommandeRepository commandeRepository;
    private final PanierRepository panierRepository;
    private final UserRepository userRepository;
    private final VariationRepository variationRepository;
    private final InvoiceService invoiceService;
    private final OrderMailService orderMailService;
    private final ActionCommandeRepository actionCommandeRepository;
    private final PaiementRepository paiementRepository;

    @Transactional
    public OrderDetailsDto checkout(String email, CheckoutRequest req) {
        User user = currentUser(email);
        Panier panier = panierRepository.findByClientId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Cart is empty"));

        if (panier.getLignes() == null || panier.getLignes().isEmpty())
            throw new ResponseStatusException(BAD_REQUEST, "Cart is empty");

        // 1. Build Commande object (without saving yet)
        Commande commande = new Commande();
        commande.setClient(user);
        commande.setNomClient(isBlank(req.getNom()) ? user.getNom() : req.getNom());
        commande.setPrenomClient(isBlank(req.getPrenom()) ? user.getPrenom() : req.getPrenom());
        commande.setEmailClient(user.getEmail());
        commande.setTelephone(req.getTelephone());
        commande.setAdresse(req.getAdresse());
        commande.setVille(req.getVille());
        commande.setCodePostal(req.getCodePostal());
        commande.setModePaiement(parseModePaiement(req.getModePaiement()));
        commande.setCardLast4(last4(req.getCardLast4()));
        commande.setD17Phone(blankToNull(req.getD17Phone()));
        commande.setD17Reference(blankToNull(req.getD17Reference()));
        commande.setBankReference(blankToNull(req.getBankReference()));
        commande.setNote(blankToNull(req.getNote()));
        commande.setSignatureDataUrl(blankToNull(req.getSignatureDataUrl()));
        commande.setSignedAt(isBlank(req.getSignatureDataUrl()) ? null : LocalDateTime.now());
        commande.setArchived(false);
        commande.setStatutCommande(StatutCommande.EN_ATTENTE);
        if (commande.getModePaiement() == ModePaiement.SIMULE) {
            commande.setStatutPaiement(StatutPaiement.ACCEPTE);
            commande.setPaymentInstructions("✅ Paiement simulé (PFE). L'administrateur doit confirmer la commande.");
        } else if (commande.getModePaiement() == ModePaiement.LIVRAISON) {
            commande.setStatutPaiement(StatutPaiement.NON_REQUIS);
            commande.setPaymentInstructions("Paiement à la livraison.");
        } else {
            commande.setStatutPaiement(StatutPaiement.EN_ATTENTE_VERIFICATION);
            commande.setPaymentInstructions(buildPaymentInstructions(commande));
        }

        double total = 0.0;
        List<VariationArticle> variationsToUpdate = new ArrayList<>();

        for (LignePanier lignePanier : panier.getLignes()) {
            if (lignePanier.getVariationId() == null)
                throw new ResponseStatusException(BAD_REQUEST, "Missing variation for one cart line");

            int requestedQty = Math.max(1, lignePanier.getQuantite());
            VariationArticle variation = variationRepository.findById(lignePanier.getVariationId())
                    .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Variation not found"));

            Integer stockValue = variation.getQuantiteStock();
            int currentStock = stockValue == null ? 0 : stockValue;
            if (currentStock < requestedQty)
                throw new ResponseStatusException(BAD_REQUEST, "Insufficient stock for " + lignePanier.getNomProduit());

            // Defer stock update until after all validations (but we apply now; transaction ensures rollback if any later failure)
            variationsToUpdate.add(variation);
            variation.setQuantiteStock(currentStock - requestedQty);

            LigneCommande ligneCommande = new LigneCommande();
            ligneCommande.setArticleId(lignePanier.getArticleId());
            ligneCommande.setVariationId(lignePanier.getVariationId());
            ligneCommande.setNomProduit(lignePanier.getNomProduit());
            ligneCommande.setImageUrl(lignePanier.getImageUrl());
            ligneCommande.setCouleurNom(lignePanier.getCouleurNom());
            ligneCommande.setTaillePointure(lignePanier.getTaillePointure());
            ligneCommande.setQuantite(requestedQty);
            ligneCommande.setPrixUnitaire(lignePanier.getPrixUnitaire());
            ligneCommande.setSousTotal(lignePanier.getPrixUnitaire() * requestedQty);
            total += ligneCommande.getSousTotal();
            commande.addLigne(ligneCommande);
        }
        commande.setTotal(total);

        // 2. Save order (stock already updated in variations, but they are managed)
        Commande saved = commandeRepository.save(commande);

        // 3. Create payment record for SIMULE
        if (saved.getModePaiement() == ModePaiement.SIMULE) {
            Paiement paiement = new Paiement(saved, saved.getTotal(), saved.getModePaiement(),
                    StatutPaiement.ACCEPTE, "SIMULE_" + saved.getReferenceCommande(), "Paiement simulé");
            paiementRepository.save(paiement);
        }

        // 4. Log action
        logAction(saved, user, TypeActionCommande.CREATION_COMMANDE, null,
                saved.getStatutCommande().name(), "Commande créée depuis le checkout");

        // 5. Generate invoice
        if (isBlank(saved.getInvoiceNumber()))
            saved.setInvoiceNumber(saved.getReferenceCommande());
        saved.setInvoiceUrl(invoiceService.generateProformaInvoice(saved));
        saved = commandeRepository.save(saved);

        // 6. Clear cart
        panier.clearLignes();
        panier.setDateMaj(LocalDateTime.now());
        panierRepository.save(panier);

        // 7. Send email (errors logged but do not rollback)
        try {
            orderMailService.sendInvoiceEmail(saved);
        } catch (Exception e) {
            log.error("Failed to send invoice email for order {}: {}", saved.getId(), e.getMessage(), e);
        }

        return toDetailsDto(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryDto> myOrders(String email, Boolean archived) {
        User user = currentUser(email);
        List<Commande> commandes = archived == null
                ? commandeRepository.findByClientIdOrderByDateCommandeDesc(user.getId())
                : commandeRepository.findByClientIdAndArchivedOrderByDateCommandeDesc(user.getId(), archived);
        return commandes.stream().map(this::toSummaryDto).toList();
    }

    @Transactional(readOnly = true)
    public OrderDetailsDto details(String email, Long id) {
        User user = currentUser(email);
        Commande commande = commandeRepository.findByIdAndClientId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Order not found"));
        return toDetailsDto(commande);
    }

    @Transactional
    public OrderDetailsDto cancel(String email, Long id) {
        User user = currentUser(email);
        Commande commande = commandeRepository.findByIdAndClientId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Order not found"));

        if (commande.getStatutCommande() != StatutCommande.EN_ATTENTE) {
            throw new ResponseStatusException(BAD_REQUEST, "This order can no longer be cancelled");
        }

        String ancienStatut = commande.getStatutCommande().name();
        commande.setStatutCommande(StatutCommande.ANNULEE);
        Commande saved = commandeRepository.save(commande);

        logAction(saved, user, TypeActionCommande.ANNULATION_COMMANDE,
                ancienStatut, saved.getStatutCommande().name(),
                "Commande annulée par le client");
        return toDetailsDto(saved);
    }

    @Transactional
    public OrderDetailsDto archive(String email, Long id) {
        User user = currentUser(email);
        Commande commande = commandeRepository.findByIdAndClientId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Order not found"));
        commande.setArchived(true);
        Commande saved = commandeRepository.save(commande);
        logAction(saved, user, TypeActionCommande.ARCHIVAGE_COMMANDE,
                null, null, "Commande archivée par le client");
        return toDetailsDto(saved);
    }

    // --------------------- PRIVATE HELPERS ---------------------

    private void logAction(Commande commande, User user, TypeActionCommande typeAction,
                           String ancienStatut, String nouveauStatut, String details) {
        ActionCommande action = new ActionCommande();
        action.setCommande(commande);
        action.setUtilisateur(user);
        action.setTypeAction(typeAction);
        action.setAncienStatut(ancienStatut);
        action.setNouveauStatut(nouveauStatut);
        action.setDetails(details);
        actionCommandeRepository.save(action);
    }

    private User currentUser(String email) {
        if (email == null || email.isBlank())
            throw new ResponseStatusException(UNAUTHORIZED, "Unauthorized");
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Unauthorized"));
    }

    private ModePaiement parseModePaiement(String value) {
        if (value == null || value.isBlank()) return ModePaiement.LIVRAISON;
        try {
            return ModePaiement.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ModePaiement.LIVRAISON;
        }
    }

    private String buildPaymentInstructions(Commande c) {
        if (c.getModePaiement() == null) {
            return "Suivez les instructions de paiement envoyées par email.";
        }
        return switch (c.getModePaiement()) {
            case LIVRAISON -> "Paiement à la livraison.";
            case CARTE -> {
                String last4 = isBlank(c.getCardLast4()) ? "" : " - carte ****" + c.getCardLast4();
                yield "Paiement par carte" + last4 + ".";
            }
            case D17 -> "Veuillez effectuer le paiement via D17 et conserver la référence de paiement.";
            case VIREMENT -> "Veuillez effectuer le virement bancaire et conserver la référence du virement.";
            case SIMULE -> "✅ Paiement simulé (PFE). Aucune action requise.";
        };
    }

    private String last4(String value) {
        if (isBlank(value)) return null;
        String digits = value.replaceAll("\\s+", "");
        return digits.length() <= 4 ? digits : digits.substring(digits.length() - 4);
    }

    private boolean isBlank(String v) {
        return v == null || v.isBlank();
    }

    private String blankToNull(String v) {
        return isBlank(v) ? null : v.trim();
    }

    private OrderSummaryDto toSummaryDto(Commande c) {
        OrderSummaryDto d = new OrderSummaryDto();
        d.setId(c.getId());
        d.setReferenceCommande(c.getReferenceCommande());
        d.setDateCommande(c.getDateCommande());
        d.setStatutCommande(c.getStatutCommande() != null ? c.getStatutCommande().name() : null);
        d.setTotal(c.getTotal());
        d.setNombreLignes(c.getLignes() != null ? c.getLignes().size() : 0);
        d.setArchived(c.isArchived());
        return d;
    }

    private OrderDetailsDto toDetailsDto(Commande c) {
        OrderDetailsDto d = new OrderDetailsDto();
        d.setId(c.getId());
        d.setReferenceCommande(c.getReferenceCommande());
        d.setDateCommande(c.getDateCommande());
        d.setStatutCommande(c.getStatutCommande() != null ? c.getStatutCommande().name() : null);
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
}