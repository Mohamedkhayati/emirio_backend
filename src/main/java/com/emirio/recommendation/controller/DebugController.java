package com.emirio.recommendation.controller;

import com.emirio.cart.TypeActionPanier;
import com.emirio.cart.repo.InteractionPanierRepository;
import com.emirio.favorite.FavoriteRepository;
import com.emirio.order.repo.LigneCommandeRepository;
import com.emirio.user.User;
import com.emirio.user.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private final UserRepository userRepository;
    private final FavoriteRepository favoriteRepository;
    private final InteractionPanierRepository interactionPanierRepository;
    private final LigneCommandeRepository ligneCommandeRepository;

    public DebugController(UserRepository userRepository,
                           FavoriteRepository favoriteRepository,
                           InteractionPanierRepository interactionPanierRepository,
                           LigneCommandeRepository ligneCommandeRepository) {
        this.userRepository = userRepository;
        this.favoriteRepository = favoriteRepository;
        this.interactionPanierRepository = interactionPanierRepository;
        this.ligneCommandeRepository = ligneCommandeRepository;
    }

    // 🔓 PUBLIC endpoint – only for testing (remove in production)
    @GetMapping("/public-interactions")
    public Map<String, Object> publicDebug(@RequestParam String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        Map<String, Object> result = new HashMap<>();
        result.put("favorites", favoriteRepository.findByUser(user).size());
        result.put("cartAdds", interactionPanierRepository.findByUtilisateurAndTypeAction(user, TypeActionPanier.AJOUT_ARTICLE).size());
        result.put("purchases", ligneCommandeRepository.findByCommandeClient(user).size());
        return result;
    }
    
}