package com.emirio.analytics.dto;

import java.util.Map;

public class OrdersStats {
    private long totalOrders;
    private double totalRevenue;
    private double averageOrderValue;
    private Map<String, Long> ordersByStatus;
    private Map<String, Long> ordersByPaymentStatus;

    public OrdersStats(long totalOrders, double totalRevenue, double averageOrderValue,
                       Map<String, Long> ordersByStatus, Map<String, Long> ordersByPaymentStatus) {
        this.totalOrders = totalOrders;
        this.totalRevenue = totalRevenue;
        this.averageOrderValue = averageOrderValue;
        this.ordersByStatus = ordersByStatus;
        this.ordersByPaymentStatus = ordersByPaymentStatus;
    }

    public long getTotalOrders() { return totalOrders; }
    public double getTotalRevenue() { return totalRevenue; }
    public double getAverageOrderValue() { return averageOrderValue; }
    public Map<String, Long> getOrdersByStatus() { return ordersByStatus; }
    public Map<String, Long> getOrdersByPaymentStatus() { return ordersByPaymentStatus; }
}