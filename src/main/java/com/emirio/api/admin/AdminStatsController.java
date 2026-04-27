package com.emirio.api.admin;

import com.emirio.analytics.StatisticsService;
import com.emirio.analytics.dto.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {
    private static final Logger log = LoggerFactory.getLogger(AdminStatsController.class);
    private final StatisticsService statsService;

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
}