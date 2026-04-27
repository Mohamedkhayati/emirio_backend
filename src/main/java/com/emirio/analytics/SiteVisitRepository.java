package com.emirio.analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

public interface SiteVisitRepository extends JpaRepository<SiteVisit, Long> {
    long countByVisitTimeBetween(LocalDateTime start, LocalDateTime end);
}