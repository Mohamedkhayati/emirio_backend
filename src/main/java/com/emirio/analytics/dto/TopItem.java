package com.emirio.analytics.dto;

public class TopItem {
    private String name;
    private long quantity;
    private double revenue;

    public TopItem(String name, long quantity, double revenue) {
        this.name = name;
        this.quantity = quantity;
        this.revenue = revenue;
    }

    public String getName() { return name; }
    public long getQuantity() { return quantity; }
    public double getRevenue() { return revenue; }
}