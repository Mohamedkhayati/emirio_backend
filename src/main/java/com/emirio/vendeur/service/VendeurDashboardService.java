package com.emirio.vendeur.service;

import com.emirio.order.repo.LigneCommandeRepository;
import com.emirio.security.CurrentUserService;
import com.emirio.user.User;
import com.emirio.vendeur.dto.DashboardStatsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VendeurDashboardService {

    private final LigneCommandeRepository ligneCommandeRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats() {
        User vendeur = currentUserService.requireCurrentUser();
        Long vendeurId = vendeur.getId();

        DashboardStatsDto stats = new DashboardStatsDto();

        // Total sales (sum of sousTotal for lines belonging to vendeur's articles)
        Double totalSales = ligneCommandeRepository.sumSousTotalByVendeurId(vendeurId);
        stats.setTotalSales(totalSales != null ? totalSales : 0.0);

        // Total orders count (distinct orders containing vendeur's articles)
        Long totalOrders = ligneCommandeRepository.countDistinctOrdersByVendeurId(vendeurId);
        stats.setTotalOrders(totalOrders != null ? totalOrders : 0L);

        // Total items sold (sum of quantities)
        Long totalItemsSold = ligneCommandeRepository.sumQuantitiesByVendeurId(vendeurId);
        stats.setTotalItemsSold(totalItemsSold != null ? totalItemsSold : 0L);

        // Top 5 selling articles
        List<Object[]> topArticlesRaw = ligneCommandeRepository.findTopArticlesByVendeurId(vendeurId);
        List<DashboardStatsDto.TopArticleDto> topArticles = topArticlesRaw.stream()
                .limit(5)
                .map(row -> {
                    DashboardStatsDto.TopArticleDto dto = new DashboardStatsDto.TopArticleDto();
                    dto.setArticleId(((Number) row[0]).longValue());
                    dto.setArticleNom((String) row[1]);
                    dto.setTotalQuantitySold(((Number) row[2]).longValue());
                    dto.setTotalRevenue(((Number) row[3]).doubleValue());
                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());
        stats.setTopArticles(topArticles);

        // Top 5 selling categories
        List<Object[]> topCategoriesRaw = ligneCommandeRepository.findTopCategoriesByVendeurId(vendeurId);
        List<DashboardStatsDto.TopCategoryDto> topCategories = topCategoriesRaw.stream()
                .limit(5)
                .map(row -> {
                    DashboardStatsDto.TopCategoryDto dto = new DashboardStatsDto.TopCategoryDto();
                    dto.setCategoryId(((Number) row[0]).longValue());
                    dto.setCategoryNom((String) row[1]);
                    dto.setTotalQuantitySold(((Number) row[2]).longValue());
                    dto.setTotalRevenue(((Number) row[3]).doubleValue());
                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());
        stats.setTopCategories(topCategories);

        return stats;
    }
}