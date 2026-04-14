package com.emirio.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final UserDetailsService uds;

    public JwtAuthFilter(JwtService jwt, UserDetailsService uds) {
        this.jwt = jwt;
        this.uds = uds;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        return HttpMethod.OPTIONS.matches(request.getMethod())
                || path.startsWith("/api/auth/")
                || path.startsWith("/oauth2/")
                || path.startsWith("/login/")
                || path.startsWith("/error")
                || path.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {

        String path = req.getRequestURI();
        String method = req.getMethod();
        String authHeader = req.getHeader("Authorization");

        System.out.println("\n=== JWT FILTER START ===");
        System.out.println("URI = " + path);
        System.out.println("Method = " + method);
        System.out.println("Authorization header = " + authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("No Bearer token found, skipping JWT auth.");
            System.out.println("=== JWT FILTER END ===\n");
            chain.doFilter(req, res);
            return;
        }

        try {
            String token = authHeader.substring(7);
            System.out.println("Token received, length = " + token.length());

            String email = jwt.getSubject(token);
            System.out.println("JWT subject/email = " + email);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = uds.loadUserByUsername(email);

                System.out.println("Loaded UserDetails username = " + userDetails.getUsername());
                System.out.println("Authorities from UserDetails = " + userDetails.getAuthorities());

                boolean valid = jwt.isTokenValid(token, userDetails.getUsername());
                System.out.println("Token valid = " + valid);

                if (valid) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    System.out.println("Authentication stored in SecurityContext.");
                    System.out.println("SecurityContext auth = " + SecurityContextHolder.getContext().getAuthentication());
                    System.out.println("SecurityContext authorities = "
                            + SecurityContextHolder.getContext().getAuthentication().getAuthorities());
                } else {
                    System.out.println("Token invalid for username = " + userDetails.getUsername());
                }
            } else {
                System.out.println("Email is null or authentication already exists.");
                System.out.println("Existing auth = " + SecurityContextHolder.getContext().getAuthentication());
            }
        } catch (JwtException e) {
            System.out.println("JWT ERROR: " + e.getMessage());
            e.printStackTrace();
            SecurityContextHolder.clearContext();
        } catch (IllegalArgumentException e) {
            System.out.println("JWT ARGUMENT ERROR: " + e.getMessage());
            e.printStackTrace();
            SecurityContextHolder.clearContext();
        } catch (Exception e) {
            System.out.println("UNEXPECTED JWT FILTER ERROR: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            SecurityContextHolder.clearContext();
        }

        System.out.println("=== JWT FILTER END ===\n");
        chain.doFilter(req, res);
    }
}