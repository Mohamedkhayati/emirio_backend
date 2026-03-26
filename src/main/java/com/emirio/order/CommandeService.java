package com.emirio.order;

import com.emirio.cart.LignePanier;
import com.emirio.cart.Panier;
import com.emirio.cart.repo.PanierRepository;
import com.emirio.order.dto.CheckoutRequest;
import com.emirio.order.dto.CommandeResponse;
import com.emirio.order.repo.CommandeRepository;
import com.emirio.user.User;
import com.emirio.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommandeService {

    private final CommandeRepository commandeRepository;
    private final PanierRepository panierRepository;
    private final UserRepository userRepository;
    private final CommandeMailService commandeMailService;

    public CommandeService(
        CommandeRepository commandeRepository,
        PanierRepository panierRepository,
        UserRepository userRepository,
        CommandeMailService commandeMailService
    ) {
        this.commandeRepository = commandeRepository;
        this.panierRepository = panierRepository;
        this.userRepository = userRepository;
        this.commandeMailService = commandeMailService;
    }

    private User currentUser(String email) {
        return userRepository.findByEmail(email).orElseThrow();
    }

    @Transactional
    public CommandeResponse checkout(String email, CheckoutRequest req) {
        User user = currentUser(email);

        Panier panier = panierRepository.findByClientId(user.getId())
            .orElseThrow(() -> new IllegalStateException("Panier introuvable"));

        if (panier.getLignes().isEmpty()) {
            throw new IllegalStateException("Panier vide");
        }

        Commande commande = new Commande();
        commande.setClient(user);
        commande.setNomClient(
            req.getNom() != null && !req.getNom().isBlank() ? req.getNom() : user.getNom()
        );
        commande.setPrenomClient(
            req.getPrenom() != null && !req.getPrenom().isBlank() ? req.getPrenom() : user.getPrenom()
        );
        commande.setEmailClient(user.getEmail());
        commande.setTelephone(req.getTelephone());
        commande.setAdresse(req.getAdresse());
        commande.setVille(req.getVille());
        commande.setCodePostal(req.getCodePostal());
        commande.setModePaiement(req.getModePaiement());
        commande.setCardLast4(req.getCardLast4());
        commande.setNote(req.getNote());
        commande.setStatutCommande(StatutCommande.EN_ATTENTE);

        double total = 0.0;

        for (LignePanier lp : panier.getLignes()) {
            LigneCommande lc = new LigneCommande();
            lc.setArticleId(lp.getArticleId());
            lc.setVariationId(lp.getVariationId());
            lc.setNomProduit(lp.getNomProduit());
            lc.setImageUrl(lp.getImageUrl());
            lc.setCouleurNom(lp.getCouleurNom());
            lc.setTaillePointure(lp.getTaillePointure());
            lc.setQuantite(lp.getQuantite());
            lc.setPrixUnitaire(lp.getPrixUnitaire());
            lc.setSousTotal(lp.getSousTotal());

            total += lc.getSousTotal();
            commande.addLigne(lc);
        }

        commande.setTotal(total);

        Commande saved = commandeRepository.save(commande);

        panier.getLignes().clear();
        panierRepository.save(panier);

        try {
            commandeMailService.sendCommandePassee(saved);
        } catch (Exception ignored) {
        }

        return CommandeResponse.from(saved);
    }

    @Transactional
    public List<CommandeResponse> myOrders(String email, Boolean archived) {
        User user = currentUser(email);

        List<Commande> commandes = archived == null
            ? commandeRepository.findByClientIdOrderByDateCommandeDesc(user.getId())
            : commandeRepository.findByClientIdAndArchivedOrderByDateCommandeDesc(user.getId(), archived);

        return commandes.stream().map(CommandeResponse::from).toList();
    }

    @Transactional
    public CommandeResponse archiveMyOrder(String email, Long id) {
        User user = currentUser(email);

        Commande commande = commandeRepository.findByIdAndClientId(id, user.getId())
            .orElseThrow(() -> new IllegalArgumentException("Commande introuvable"));

        commande.setArchived(true);
        return CommandeResponse.from(commandeRepository.save(commande));
    }
}