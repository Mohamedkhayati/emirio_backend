package com.emirio.analytics;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "site_visit")
public class SiteVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "visit_time", nullable = false)
    private LocalDateTime visitTime;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "page_url")
    private String pageUrl;

    @Column(name = "ip_address")
    private String ipAddress;

    public SiteVisit() {}

    public SiteVisit(String sessionId, String pageUrl, String ipAddress) {
        this.visitTime = LocalDateTime.now();
        this.sessionId = sessionId;
        this.pageUrl = pageUrl;
        this.ipAddress = ipAddress;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getVisitTime() { return visitTime; }
    public void setVisitTime(LocalDateTime visitTime) { this.visitTime = visitTime; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getPageUrl() { return pageUrl; }
    public void setPageUrl(String pageUrl) { this.pageUrl = pageUrl; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
}