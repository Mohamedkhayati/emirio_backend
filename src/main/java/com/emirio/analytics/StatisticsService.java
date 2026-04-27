package com.emirio.analytics;

import com.emirio.analytics.dto.*;
import com.emirio.order.Commande;
import com.emirio.order.repo.CommandeRepository;
import com.emirio.catalog.Article;
import com.emirio.catalog.repo.ArticleRepository;
import com.emirio.catalog.Category;
import com.emirio.catalog.repo.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StatisticsService {
    private static final Logger log = LoggerFactory.getLogger(StatisticsService.class);
    
    private final SiteVisitRepository visitRepository;
    private final CommandeRepository commandeRepository;
    private final ArticleRepository articleRepository;
    private final CategoryRepository categoryRepository;

    public VisitsStats getVisitsStats() {
        try {
            LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
            LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
            long today = visitRepository.countByVisitTimeBetween(startOfDay, endOfDay);
            long last30 = visitRepository.countByVisitTimeBetween(LocalDateTime.now().minusDays(30), LocalDateTime.now());
            long total = visitRepository.count();
            log.info("Visits -> total: {}, today: {}, last30: {}", total, today, last30);
            return new VisitsStats(total, today, last30);
        } catch (Exception e) {
            log.error("Error counting visits", e);
            return new VisitsStats(0, 0, 0);
        }
    }

    public OrdersStats getOrdersStats() {
        try {
            List<Commande> all = commandeRepository.findAll();
            long totalOrders = all.size();
            double totalRevenue = all.stream().mapToDouble(Commande::getTotal).sum();
            double avg = totalOrders == 0 ? 0 : totalRevenue / totalOrders;
            Map<String, Long> byStatus = new HashMap<>();
            Map<String, Long> byPayment = new HashMap<>();
            all.forEach(c -> {
                byStatus.merge(c.getStatutCommande().name(), 1L, Long::sum);
                byPayment.merge(c.getStatutPaiement().name(), 1L, Long::sum);
            });
            return new OrdersStats(totalOrders, totalRevenue, avg, byStatus, byPayment);
        } catch (Exception e) {
            log.error("Orders stats error", e);
            return new OrdersStats(0, 0, 0, new HashMap<>(), new HashMap<>());
        }
    }
    public SalesOverTime getDailySales(int days) {
        try {
            LocalDateTime end = LocalDateTime.now();
            LocalDateTime start = end.minusDays(days);
            List<Object[]> results = commandeRepository.findSalesGroupedByDate(start, end);
            List<String> labels = new ArrayList<>();
            List<Double> sales = new ArrayList<>();
            for (int i = 0; i < days; i++) {
                labels.add(LocalDate.now().minusDays(days - 1 - i).toString());
                sales.add(0.0);
            }
            if (results != null) {
                for (Object[] row : results) {
                    if (row[0] == null || row[1] == null) continue;
                    LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
                    double sum = ((Number) row[1]).doubleValue();
                    int index = (int) (LocalDate.now().toEpochDay() - date.toEpochDay());
                    if (index >= 0 && index < days) {
                        sales.set(days - 1 - index, sum);
                    }
                }
            }
            return new SalesOverTime("daily", labels, sales);
        } catch (Exception e) {
            log.error("Error in getDailySales", e);
            return new SalesOverTime("daily", new ArrayList<>(), new ArrayList<>());
        }
    }

    public List<TopItem> getTopSellingArticles(int limit) {
        try {
            List<Object[]> results = commandeRepository.findTopArticlesByQuantity(limit);
            List<TopItem> items = new ArrayList<>();
            if (results != null) {
                for (Object[] row : results) {
                    Long articleId = (Long) row[0];
                    long qty = ((Number) row[1]).longValue();
                    double revenue = ((Number) row[2]).doubleValue();
                    String name = articleRepository.findById(articleId)
                            .map(Article::getNom)
                            .orElse("Article #" + articleId);
                    items.add(new TopItem(name, qty, revenue));
                }
            }
            return items;
        } catch (Exception e) {
            log.error("Error in getTopSellingArticles", e);
            return new ArrayList<>();
        }
    }

    public List<TopItem> getTopCategories(int limit) {
        try {
            List<Object[]> results = commandeRepository.findTopCategoriesByRevenue(limit);
            List<TopItem> items = new ArrayList<>();
            if (results != null) {
                for (Object[] row : results) {
                    Long catId = (Long) row[0];
                    double revenue = ((Number) row[1]).doubleValue();
                    long qty = ((Number) row[2]).longValue();
                    String name = categoryRepository.findById(catId)
                            .map(Category::getNom)
                            .orElse("Category #" + catId);
                    items.add(new TopItem(name, qty, revenue));
                }
            }
            return items;
        } catch (Exception e) {
            log.error("Error in getTopCategories", e);
            return new ArrayList<>();
        }
    }
    
}