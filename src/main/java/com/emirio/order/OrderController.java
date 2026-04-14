package com.emirio.order;

import com.emirio.order.dto.CheckoutRequest;
import com.emirio.order.dto.OrderDetailsDto;
import com.emirio.order.dto.OrderSummaryDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    public OrderDetailsDto checkout(
        @Valid @RequestBody CheckoutRequest request,
        Authentication authentication
    ) {
        return orderService.checkout(authentication.getName(), request);
    }

    @GetMapping("/my")
    public List<OrderSummaryDto> myOrders(
        Authentication authentication,
        @RequestParam(required = false) Boolean archived
    ) {
        return orderService.myOrders(authentication.getName(), archived);
    }

    @GetMapping("/{id}")
    public OrderDetailsDto details(
        @PathVariable Long id,
        Authentication authentication
    ) {
        return orderService.details(authentication.getName(), id);
    }

    @PatchMapping("/{id}/cancel")
    public OrderDetailsDto cancel(
        @PathVariable Long id,
        Authentication authentication
    ) {
        return orderService.cancel(authentication.getName(), id);
    }

    @PatchMapping("/{id}/archive")
    public OrderDetailsDto archive(
        @PathVariable Long id,
        Authentication authentication
    ) {
        return orderService.archive(authentication.getName(), id);
    }
}