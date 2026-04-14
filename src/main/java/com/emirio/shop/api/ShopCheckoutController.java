package com.emirio.shop.api;

import com.emirio.shop.dto.CheckoutRequest;
import com.emirio.shop.model.ShopOrder;
import com.emirio.shop.service.ShopOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shop")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequiredArgsConstructor
public class ShopCheckoutController {

    private final ShopOrderService shopOrderService;

    @PostMapping("/checkout")
    public ShopOrder checkout(@Valid @RequestBody CheckoutRequest request) {
        return shopOrderService.checkout(request);
    }
}