package com.emirio.order;

import com.emirio.order.dto.CheckoutRequest;
import com.emirio.order.dto.OrderDetailsDto;
import com.emirio.order.dto.OrderSummaryDto;
import com.emirio.order.repo.CommandeRepository;
import com.emirio.user.User;
import com.emirio.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;
    private final CommandeRepository commandeRepository;

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

    @PatchMapping("/{id}/simulate-payment")
    @Transactional
    public ResponseEntity<?> simulatePayment(@PathVariable Long id, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName())
            .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Unauthorized"));

        Commande commande = commandeRepository.findByIdAndClientId(id, user.getId())
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Order not found"));

        if (commande.getStatutPaiement() == StatutPaiement.PAYE) {
            throw new ResponseStatusException(BAD_REQUEST, "Order already paid");
        }

        commande.setStatutPaiement(StatutPaiement.PAYE);
        commande.setStatutCommande(StatutCommande.CONFIRMEE);
        commande.setPaymentInstructions("Fake payment applied via /simulate-payment endpoint");
        commandeRepository.save(commande);

        return ResponseEntity.ok(Map.of(
            "message", "Fake payment successful",
            "orderId", id,
            "status", "PAID"
        ));
    }

    @PatchMapping("/{id}/archive")
    public OrderDetailsDto archive(
        @PathVariable Long id,
        Authentication authentication
    ) {
        return orderService.archive(authentication.getName(), id);
    }
}