package com.emirio.order.repo;

import com.emirio.order.Commande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommandeRepository extends JpaRepository<Commande, Long> {
    List<Commande> findByClientIdOrderByDateCommandeDesc(Long clientId);
    List<Commande> findByClientIdAndArchivedOrderByDateCommandeDesc(Long clientId, boolean archived);
    Optional<Commande> findByIdAndClientId(Long id, Long clientId);
    List<Commande> findAllByOrderByDateCommandeDesc();
 // Find orders that contain at least one line belonging to the seller's articles
    @Query("select distinct c from Commande c join c.lignes l where l.articleId in (select a.id from Article a where a.vendeur.id = :vendeurId) order by c.dateCommande desc")
    List<Commande> findOrdersContainingVendeurArticles(@Param("vendeurId") Long vendeurId);

    // Find a specific order and check that it contains at least one line of the seller
    @Query("select c from Commande c join c.lignes l where c.id = :orderId and l.articleId in (select a.id from Article a where a.vendeur.id = :vendeurId)")
    Optional<Commande> findByIdAndContainsVendeurArticles(@Param("orderId") Long orderId, @Param("vendeurId") Long vendeurId);
}