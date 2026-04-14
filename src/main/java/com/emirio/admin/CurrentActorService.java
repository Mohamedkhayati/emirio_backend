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

@Service
@RequiredArgsConstructor
public class CurrentActorService {

    private final UserRepository users;

    public void requireGeneralAdmin() {
        Authentication auth = currentAuthentication();

        if (auth == null || !auth.isAuthenticated() || Objects.equals(auth.getName(), "anonymousUser")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        boolean isGeneralAdmin = auth.getAuthorities().stream()
            .map(a -> a.getAuthority())
            .anyMatch("ROLE_ADMIN_GENERAL"::equals);

        if (!isGeneralAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only general admin can perform this action");
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