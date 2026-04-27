package com.emirio.order.repo;

import com.emirio.order.Commande;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommandeRepository extends JpaRepository<Commande, Long> {

    List<Commande> findByClientIdOrderByDateCommandeDesc(Long clientId);
    List<Commande> findByClientIdAndArchivedOrderByDateCommandeDesc(Long clientId, boolean archived);
    Optional<Commande> findByIdAndClientId(Long id, Long clientId);
    List<Commande> findAllByOrderByDateCommandeDesc();

    @Query("select distinct c from Commande c join c.lignes l where l.articleId in (select a.id from Article a where a.vendeur.id = :vendeurId) order by c.dateCommande desc")
    List<Commande> findOrdersContainingVendeurArticles(@Param("vendeurId") Long vendeurId);

    @Query("select c from Commande c join c.lignes l where c.id = :orderId and l.articleId in (select a.id from Article a where a.vendeur.id = :vendeurId)")
    Optional<Commande> findByIdAndContainsVendeurArticles(@Param("orderId") Long orderId, @Param("vendeurId") Long vendeurId);

    // Daily sales (works if dateCommande is LocalDateTime)
    @Query("SELECT FUNCTION('DATE', c.dateCommande) as date, SUM(c.total) FROM Commande c " +
           "WHERE c.dateCommande BETWEEN :start AND :end GROUP BY FUNCTION('DATE', c.dateCommande) ORDER BY date")
    List<Object[]> findSalesGroupedByDate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Top articles – using quantite and prixUnitaire from LigneCommande
    @Query("SELECT l.articleId, SUM(l.quantite), SUM(l.prixUnitaire * l.quantite) FROM LigneCommande l " +
           "GROUP BY l.articleId ORDER BY SUM(l.quantite) DESC")
    List<Object[]> findTopArticlesByQuantity(Pageable pageable);

    default List<Object[]> findTopArticlesByQuantity(int limit) {
        return findTopArticlesByQuantity(PageRequest.of(0, limit));
    }

    // Top categories – 'categorie' is the field in Article (French)
    @Query("SELECT a.categorie.id, SUM(l.prixUnitaire * l.quantite), SUM(l.quantite) FROM LigneCommande l " +
           "JOIN Article a ON l.articleId = a.id WHERE a.categorie IS NOT NULL " +
           "GROUP BY a.categorie.id ORDER BY SUM(l.prixUnitaire * l.quantite) DESC")
    List<Object[]> findTopCategoriesByRevenue(Pageable pageable);

    default List<Object[]> findTopCategoriesByRevenue(int limit) {
        return findTopCategoriesByRevenue(PageRequest.of(0, limit));
    }
}