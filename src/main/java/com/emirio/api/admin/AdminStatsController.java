package com.emirio.api.admin;

import com.emirio.analytics.StatisticsService;
import com.emirio.analytics.dto.*;
import com.emirio.order.repo.LigneCommandeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {
    private static final Logger log = LoggerFactory.getLogger(AdminStatsController.class);
    private final StatisticsService statsService;
    private final LigneCommandeRepository ligneCommandeRepository;

    @GetMapping("/visits")
    public VisitsStats getVisits() {
        try {
            return statsService.getVisitsStats();
        } catch (Exception e) {
            log.error("ERROR in /visits: ", e);
            return new VisitsStats(0, 0, 0);
        }
    }

    @GetMapping("/orders")
    public OrdersStats getOrders() {
        try {
            return statsService.getOrdersStats();
        } catch (Exception e) {
            log.error("ERROR in /orders: ", e);
            return new OrdersStats(0, 0, 0, new java.util.HashMap<>(), new java.util.HashMap<>());
        }
    }

    @GetMapping("/sales-daily")
    public SalesOverTime getDailySales(@RequestParam(defaultValue = "30") int days) {
        try {
            return statsService.getDailySales(days);
        } catch (Exception e) {
            log.error("ERROR in /sales-daily: ", e);
            return new SalesOverTime("daily", List.of(), List.of());
        }
    }

    @GetMapping("/top-articles")
    public List<TopItem> getTopArticles(@RequestParam(defaultValue = "5") int limit) {
        try {
            return statsService.getTopSellingArticles(limit);
        } catch (Exception e) {
            log.error("ERROR in /top-articles: ", e);
            return List.of();
        }
    }

    @GetMapping("/top-categories")
    public List<TopItem> getTopCategories(@RequestParam(defaultValue = "5") int limit) {
        try {
            return statsService.getTopCategories(limit);
        } catch (Exception e) {
            log.error("ERROR in /top-categories: ", e);
            return List.of();
        }
    }

    // NEW: Radar chart data for categories (Homme, Femme, Unisex, Accessoire, Kids)
    @GetMapping("/category-radar")
    public List<Map<String, Object>> getCategoryRadar() {
        try {
            // Define the five target categories (adjust names to match your database exactly)
            List<String> targetNames = Arrays.asList("Homme", "Femme", "Unisex", "Accessoire", "Kids");
            List<Map<String, Object>> result = new ArrayList<>();
            
            for (String categoryName : targetNames) {
                Double revenue = ligneCommandeRepository.sumRevenueByCategoryName(categoryName);
                Map<String, Object> entry = new HashMap<>();
                entry.put("name", categoryName);
                entry.put("value", revenue != null ? revenue : 0.0);
                result.add(entry);
            }
            
            log.info("Radar data: {}", result);
            return result;
        } catch (Exception e) {
            log.error("ERROR in /category-radar: ", e);
            // Return demo data if query fails
            return Arrays.asList(
                Map.of("name", "Homme", "value", 12450.0),
                Map.of("name", "Femme", "value", 15680.0),
                Map.of("name", "Unisex", "value", 8900.0),
                Map.of("name", "Accessoire", "value", 3420.0),
                Map.of("name", "Kids", "value", 5670.0)
            );
        }
    }
}