package com.emirio.security;

import com.emirio.user.Role;
import com.emirio.user.User;
import com.emirio.user.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public OAuth2LoginSuccessHandler(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException, ServletException {

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attrs = oauth2User.getAttributes();

        String email = getEmail(attrs);
        String fullName = getName(attrs);

        if (email == null || email.isBlank()) {
            response.sendRedirect(frontendUrl + "/auth?error=" + encode("Email not provided by social login"));
            return;
        }

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email.trim().toLowerCase());

            String[] names = splitName(fullName);
            u.setPrenom(names[0]);
            u.setNom(names[1]);

            u.setRole(Role.USER);
            u.setStatutCompte("ACTIVE");
            u.setMdp("SOCIAL_LOGIN");

            return userRepository.save(u);
        });

        String token = jwtService.generateToken(user.getEmail());
        response.sendRedirect(frontendUrl + "/auth?socialToken=" + encode(token));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String getEmail(Map<String, Object> attrs) {
        Object email = attrs.get("email");
        return email != null ? email.toString() : null;
    }

    private String getName(Map<String, Object> attrs) {
        Object name = attrs.get("name");
        if (name != null) return name.toString();

        Object firstName = attrs.get("given_name");
        Object lastName = attrs.get("family_name");
        if (firstName != null || lastName != null) {
            return ((firstName != null ? firstName.toString() : "") + " " +
                    (lastName != null ? lastName.toString() : "")).trim();
        }
        return "User";
    }

    private String[] splitName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return new String[]{"User", "Social"};
        }

        String[] parts = fullName.trim().split("\\s+", 2);
        String prenom = parts[0];
        String nom = parts.length > 1 ? parts[1] : "User";
        return new String[]{prenom, nom};
    }
}