package com.emirio.order.dto;

import com.emirio.order.Commande;
import com.emirio.order.LigneCommande;
import com.emirio.order.StatutCommande;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommandeResponse {

    private Long id;
    private String referenceCommande;
    private LocalDateTime dateCommande;
    private StatutCommande statutCommande;
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
    private String note;
    private List<LigneCommandeResponse> lignes;

    @Data
    public static class LigneCommandeResponse {
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

        public static LigneCommandeResponse from(LigneCommande l) {
            LigneCommandeResponse dto = new LigneCommandeResponse();
            dto.setId(l.getId());
            dto.setArticleId(l.getArticleId());
            dto.setVariationId(l.getVariationId());
            dto.setNomProduit(l.getNomProduit());
            dto.setImageUrl(l.getImageUrl());
            dto.setCouleurNom(l.getCouleurNom());
            dto.setTaillePointure(l.getTaillePointure());
            dto.setQuantite(l.getQuantite());
            dto.setPrixUnitaire(l.getPrixUnitaire());
            dto.setSousTotal(l.getSousTotal());
            return dto;
        }
    }

    public static CommandeResponse from(Commande c) {
        CommandeResponse dto = new CommandeResponse();
        dto.setId(c.getId());
        dto.setReferenceCommande(c.getReferenceCommande());
        dto.setDateCommande(c.getDateCommande());
        dto.setStatutCommande(c.getStatutCommande());
        dto.setTotal(c.getTotal());
        dto.setArchived(c.isArchived());
        dto.setNomClient(c.getNomClient());
        dto.setPrenomClient(c.getPrenomClient());
        dto.setEmailClient(c.getEmailClient());
        dto.setTelephone(c.getTelephone());
        dto.setAdresse(c.getAdresse());
        dto.setVille(c.getVille());
        dto.setCodePostal(c.getCodePostal());
        dto.setModePaiement(c.getModePaiement());
        dto.setNote(c.getNote());
        dto.setLignes(
            c.getLignes().stream()
                .map(LigneCommandeResponse::from)
                .toList()
        );
        return dto;
    }
}