package com.emirio.admin;

import com.emirio.user.User;
import com.emirio.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CurrentActorService {

    private final UserRepository users;

    public void requireGeneralAdmin() {
        Authentication auth = currentAuthentication();

        if (auth == null || !auth.isAuthenticated() || Objects.equals(auth.getName(), "anonymousUser")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        Set<String> authorities = auth.getAuthorities().stream()
            .map(a -> a.getAuthority())
            .collect(Collectors.toSet());

        boolean allowed =
            authorities.contains("ROLE_ADMIN_GENERAL")
            || authorities.contains("ROLE_GENERAL_ADMIN")
            || authorities.contains("ADMIN_GENERAL")
            || authorities.contains("GENERAL_ADMIN")
            || authorities.contains("ROLE_VENDEUR");

        if (!allowed) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Access denied. Authorities=" + authorities
            );
        }
    }

    public String currentActorEmailOrSystem() {
        Authentication auth = currentAuthentication();
        if (auth == null || !auth.isAuthenticated() || Objects.equals(auth.getName(), "anonymousUser")) {
            return "SYSTEM";
        }
        return auth.getName();
    }

    public User requireCurrentUser() {
        Authentication auth = currentAuthentication();

        if (auth == null || !auth.isAuthenticated() || Objects.equals(auth.getName(), "anonymousUser")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        return users.findByEmailIgnoreCase(auth.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }

    private Authentication currentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}