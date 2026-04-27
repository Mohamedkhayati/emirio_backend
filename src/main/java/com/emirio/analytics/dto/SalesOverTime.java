package com.emirio.analytics.dto;

import java.util.List;

public class SalesOverTime {
    private String period;
    private List<String> labels;
    private List<Double> sales;

    public SalesOverTime(String period, List<String> labels, List<Double> sales) {
        this.period = period;
        this.labels = labels;
        this.sales = sales;
    }

    public String getPeriod() { return period; }
    public List<String> getLabels() { return labels; }
    public List<Double> getSales() { return sales; }
}