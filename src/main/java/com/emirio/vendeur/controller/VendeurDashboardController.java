package com.emirio.vendeur.controller;

import com.emirio.vendeur.dto.DashboardStatsDto;
import com.emirio.vendeur.service.VendeurDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vendeur/dashboard")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequiredArgsConstructor
public class VendeurDashboardController {

    private final VendeurDashboardService dashboardService;

    @GetMapping("/stats")
    public DashboardStatsDto getStats() {
        return dashboardService.getDashboardStats();
    }
}