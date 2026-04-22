package com.emirio.vendeur.service;

import com.emirio.order.Commande;
import com.emirio.order.LigneCommande;
import com.emirio.order.repo.CommandeRepository;
import com.emirio.order.repo.LigneCommandeRepository;
import com.emirio.security.CurrentUserService;
import com.emirio.user.User;
import com.emirio.vendeur.dto.VendeurOrderDetailsDto;
import com.emirio.vendeur.dto.VendeurOrderLineDto;
import com.emirio.vendeur.dto.VendeurOrderSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
public class VendeurOrderService {

    private final CommandeRepository commandeRepository;
    private final LigneCommandeRepository ligneCommandeRepository;
    private final CurrentUserService currentUserService;

    private User currentVendeur() {
        return currentUserService.requireCurrentUser();
    }

    @Transactional(readOnly = true)
    public List<VendeurOrderSummaryDto> getMyOrders() {
        User vendeur = currentVendeur();
        List<Commande> orders = commandeRepository.findOrdersContainingVendeurArticles(vendeur.getId());
        return orders.stream().map(order -> {
            List<LigneCommande> vendeurLines = ligneCommandeRepository.findVendeurLinesByOrderId(order.getId(), vendeur.getId());
            VendeurOrderSummaryDto dto = new VendeurOrderSummaryDto();
            dto.setId(order.getId());
            dto.setReferenceCommande(order.getReferenceCommande());
            dto.setDateCommande(order.getDateCommande());
            dto.setStatutCommande(order.getStatutCommande() != null ? order.getStatutCommande().name() : null);
            dto.setStatutPaiement(order.getStatutPaiement() != null ? order.getStatutPaiement().name() : null);
            dto.setTotal(order.getTotal());
            dto.setNomClient(order.getNomClient());
            dto.setPrenomClient(order.getPrenomClient());
            dto.setNombreLignesVendeur(vendeurLines.size());
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VendeurOrderDetailsDto getOrderDetails(Long orderId) {
        User vendeur = currentVendeur();
        Commande order = commandeRepository.findByIdAndContainsVendeurArticles(orderId, vendeur.getId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Order not found or does not contain your articles"));

        List<LigneCommande> vendeurLines = ligneCommandeRepository.findVendeurLinesByOrderId(orderId, vendeur.getId());
        List<VendeurOrderLineDto> lineDtos = vendeurLines.stream().map(this::toLineDto).collect(Collectors.toList());

        VendeurOrderDetailsDto dto = new VendeurOrderDetailsDto();
        dto.setId(order.getId());
        dto.setReferenceCommande(order.getReferenceCommande());
        dto.setDateCommande(order.getDateCommande());
        dto.setStatutCommande(order.getStatutCommande() != null ? order.getStatutCommande().name() : null);
        dto.setStatutPaiement(order.getStatutPaiement() != null ? order.getStatutPaiement().name() : null);
        dto.setTotal(order.getTotal());
        dto.setNomClient(order.getNomClient());
        dto.setPrenomClient(order.getPrenomClient());
        dto.setEmailClient(order.getEmailClient());
        dto.setTelephone(order.getTelephone());
        dto.setAdresse(order.getAdresse());
        dto.setVille(order.getVille());
        dto.setCodePostal(order.getCodePostal());
        dto.setModePaiement(order.getModePaiement() != null ? order.getModePaiement().name() : null);
        dto.setNote(order.getNote());
        dto.setLignesVendeur(lineDtos);
        return dto;
    }

    private VendeurOrderLineDto toLineDto(LigneCommande line) {
        VendeurOrderLineDto dto = new VendeurOrderLineDto();
        dto.setId(line.getId());
        dto.setVariationId(line.getVariationId());
        dto.setArticleId(line.getArticleId());
        dto.setArticleNom(line.getNomProduit());
        dto.setCouleurNom(line.getCouleurNom());
        dto.setTaillePointure(line.getTaillePointure());
        dto.setQuantite(line.getQuantite());
        dto.setPrixUnitaire(line.getPrixUnitaire());
        dto.setSousTotal(line.getSousTotal());
        dto.setImageUrl(line.getImageUrl());
        return dto;
    }
}