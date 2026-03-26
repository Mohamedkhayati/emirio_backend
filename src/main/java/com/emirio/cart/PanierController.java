package com.emirio.cart;

import com.emirio.cart.dto.PanierResponse;
import com.emirio.cart.dto.PanierSyncRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/panier")
public class PanierController {

    private final PanierService panierService;

    public PanierController(PanierService panierService) {
        this.panierService = panierService;
    }

    @GetMapping
    public PanierResponse getMyPanier(Authentication auth) {
        return panierService.getMyPanier(auth.getName());
    }

    @PutMapping("/sync")
    public PanierResponse sync(Authentication auth, @Valid @RequestBody PanierSyncRequest request) {
        return panierService.sync(auth.getName(), request);
    }

    @DeleteMapping
    public void clear(Authentication auth) {
        panierService.clear(auth.getName());
    }
}