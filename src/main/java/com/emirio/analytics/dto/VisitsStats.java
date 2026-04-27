package com.emirio.analytics.dto;

public class VisitsStats {
    private long totalVisits;
    private long visitsToday;
    private long visitsLast30Days;

    public VisitsStats(long totalVisits, long visitsToday, long visitsLast30Days) {
        this.totalVisits = totalVisits;
        this.visitsToday = visitsToday;
        this.visitsLast30Days = visitsLast30Days;
    }

    public long getTotalVisits() { return totalVisits; }
    public long getVisitsToday() { return visitsToday; }
    public long getVisitsLast30Days() { return visitsLast30Days; }
}