package com.emirio.vendeur.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class DashboardStatsDto {
    private double totalSales;
    private long totalOrders;
    private long totalItemsSold;
    private List<TopArticleDto> topArticles;
    private List<TopCategoryDto> topCategories;

    @Data
    public static class TopArticleDto {
        private Long articleId;
        private String articleNom;
        private long totalQuantitySold;
        private double totalRevenue;
    }

    @Data
    public static class TopCategoryDto {
        private Long categoryId;
        private String categoryNom;
        private long totalQuantitySold;
        private double totalRevenue;
    }
}