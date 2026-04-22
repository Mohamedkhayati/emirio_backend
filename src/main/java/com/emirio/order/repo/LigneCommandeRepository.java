package com.emirio.order.repo;

import com.emirio.order.LigneCommande;
import com.emirio.user.User;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LigneCommandeRepository extends JpaRepository<LigneCommande, Long> {

    @Query("SELECT lc FROM LigneCommande lc JOIN FETCH lc.commande WHERE lc.commande.client = :user")
    List<LigneCommande> findByCommandeClient(@Param("user") User user);
 // Find order lines for a given order that belong to the seller's articles
    @Query("select l from LigneCommande l where l.commande.id = :orderId and l.articleId in (select a.id from Article a where a.vendeur.id = :vendeurId)")
    List<LigneCommande> findVendeurLinesByOrderId(@Param("orderId") Long orderId, @Param("vendeurId") Long vendeurId);
 // In LigneCommandeRepository.java
    @Query("select sum(l.sousTotal) from LigneCommande l where l.articleId in (select a.id from Article a where a.vendeur.id = :vendeurId)")
    Double sumSousTotalByVendeurId(@Param("vendeurId") Long vendeurId);

    @Query("select count(distinct l.commande.id) from LigneCommande l where l.articleId in (select a.id from Article a where a.vendeur.id = :vendeurId)")
    Long countDistinctOrdersByVendeurId(@Param("vendeurId") Long vendeurId);

    @Query("select sum(l.quantite) from LigneCommande l where l.articleId in (select a.id from Article a where a.vendeur.id = :vendeurId)")
    Long sumQuantitiesByVendeurId(@Param("vendeurId") Long vendeurId);

    @Query("select a.id, a.nom, sum(l.quantite), sum(l.sousTotal) from LigneCommande l join Article a on l.articleId = a.id where a.vendeur.id = :vendeurId group by a.id, a.nom order by sum(l.quantite) desc")
    List<Object[]> findTopArticlesByVendeurId(@Param("vendeurId") Long vendeurId);

    @Query("select c.id, c.nom, sum(l.quantite), sum(l.sousTotal) from LigneCommande l join Article a on l.articleId = a.id join a.categorie c where a.vendeur.id = :vendeurId group by c.id, c.nom order by sum(l.quantite) desc")
    List<Object[]> findTopCategoriesByVendeurId(@Param("vendeurId") Long vendeurId);
}