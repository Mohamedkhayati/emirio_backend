package com.emirio.catalog;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test-stock")
public class TestStockController {

    private final VariationStockService variationStockService;

    public TestStockController(VariationStockService variationStockService) {
        this.variationStockService = variationStockService;
    }

    @PostMapping("/decrease/{variationId}")
    public String decrease(@PathVariable Long variationId, @RequestParam int quantity) {
        variationStockService.decreaseStock(variationId, quantity);
        return "Stock updated";
    }
}