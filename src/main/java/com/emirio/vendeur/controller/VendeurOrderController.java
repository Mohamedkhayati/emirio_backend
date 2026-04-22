package com.emirio.vendeur.controller;

import com.emirio.vendeur.dto.VendeurOrderDetailsDto;
import com.emirio.vendeur.dto.VendeurOrderSummaryDto;
import com.emirio.vendeur.service.VendeurOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendeur/orders")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequiredArgsConstructor
public class VendeurOrderController {

    private final VendeurOrderService orderService;

    @GetMapping
    public List<VendeurOrderSummaryDto> listMyOrders() {
        return orderService.getMyOrders();
    }

    @GetMapping("/{id}")
    public VendeurOrderDetailsDto getOrderDetails(@PathVariable Long id) {
        return orderService.getOrderDetails(id);
    }
}