package com.emirio.cart;

import com.emirio.cart.dto.PanierResponse;
import com.emirio.cart.dto.PanierSyncRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class CartController {

    private final PanierService panierService;

    public CartController(PanierService panierService) {
        this.panierService = panierService;
    }

    @GetMapping
    public PanierResponse getMyCart(Authentication authentication) {
        return panierService.getMyPanier(requireEmail(authentication));
    }

    @PutMapping("/sync")
    public PanierResponse syncCart(
        Authentication authentication,
        @Valid @RequestBody PanierSyncRequest request
    ) {
        return panierService.sync(requireEmail(authentication), request);
    }

    @DeleteMapping
    public void clearCart(Authentication authentication) {
        panierService.clear(requireEmail(authentication));
    }

    private String requireEmail(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(UNAUTHORIZED, "Unauthorized");
        }
        return authentication.getName();
    }
}