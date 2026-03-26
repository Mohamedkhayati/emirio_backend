package com.emirio.order;

import com.emirio.order.dto.CheckoutRequest;
import com.emirio.order.dto.CommandeResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/commandes")
public class CommandeController {

    private final CommandeService commandeService;

    public CommandeController(CommandeService commandeService) {
        this.commandeService = commandeService;
    }

    @PostMapping("/checkout")
    public CommandeResponse checkout(Authentication auth, @Valid @RequestBody CheckoutRequest request) {
        return commandeService.checkout(auth.getName(), request);
    }

    @GetMapping("/me")
    public List<CommandeResponse> myOrders(
        Authentication auth,
        @RequestParam(required = false) Boolean archived
    ) {
        return commandeService.myOrders(auth.getName(), archived);
    }

    @PatchMapping("/{id}/archive")
    public CommandeResponse archive(Authentication auth, @PathVariable Long id) {
        return commandeService.archiveMyOrder(auth.getName(), id);
    }
}