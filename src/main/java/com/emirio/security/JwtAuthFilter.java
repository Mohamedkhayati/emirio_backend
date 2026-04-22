package com.emirio.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

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

        // 1. Explicitly ignore ALL OPTIONS requests (Pre-flight CORS)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 2. Ignore public paths
        return path.startsWith("/api/auth/")
                || path.startsWith("/oauth2/")
                || path.startsWith("/login/")
                || path.startsWith("/error")
                || path.equals("/favicon.ico")
                || path.startsWith("/api/catalog/variations/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {

        String path = req.getRequestURI();
        String method = req.getMethod();
        String authHeader = req.getHeader("Authorization");

        System.out.println("\n========== JWT FILTER DEBUG ==========");
        System.out.println("URI: " + path);
        System.out.println("Method: " + method);
        System.out.println("Auth Header: " + (authHeader != null ? "Present (starts with Bearer: " + authHeader.startsWith("Bearer ") + ")" : "NULL"));

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("❌ No valid Bearer token found");
            System.out.println("========================================\n");
            chain.doFilter(req, res);
            return;
        }

        try {
            String token = authHeader.substring(7);
            System.out.println("✅ Token extracted, length: " + token.length());
            
            String email = jwt.getSubject(token);
            String role = jwt.getRole(token);
            
            System.out.println("📧 Email from token: " + email);
            System.out.println("👤 Role from token: " + role);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                System.out.println("🔄 Loading UserDetails for: " + email);
                UserDetails userDetails = uds.loadUserByUsername(email);
                
                System.out.println("✅ UserDetails loaded:");
                System.out.println("   - Username: " + userDetails.getUsername());
                System.out.println("   - Authorities from DB: " + userDetails.getAuthorities());
                
                boolean valid = jwt.isTokenValid(token, userDetails.getUsername());
                System.out.println("🔐 Token valid: " + valid);
                
                if (valid) {
                    // Use role from token OR from UserDetails
                	 String dbRole = userDetails.getAuthorities().stream()
                             .findFirst()
                             .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                             .orElse("USER");
                	 
                	 String finalRole = (role != null && !role.isEmpty() && !role.equals("USER")) ? role : dbRole;

                     System.out.println("🎭 Final role being set: " + finalRole);
 
                    // Create authorities with the role (NO ROLE_ prefix)
                     List<SimpleGrantedAuthority> authorities = List.of(
                             new SimpleGrantedAuthority(finalRole)
                     );
                     UsernamePasswordAuthenticationToken authentication =
                             new UsernamePasswordAuthenticationToken(
                                     userDetails,
                                     null,
                                     authorities 
                             );

                     authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                     SecurityContextHolder.getContext().setAuthentication(authentication);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    System.out.println("✅ Authentication set in SecurityContext");
                    System.out.println("   - Auth name: " + SecurityContextHolder.getContext().getAuthentication().getName());
                    System.out.println("   - Auth authorities: " + SecurityContextHolder.getContext().getAuthentication().getAuthorities());
                    System.out.println("   - Auth authenticated: " + SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
                } else {
                    System.out.println("❌ Token validation failed");
                }
            } else {
                System.out.println("⚠️ Email is null or authentication already exists");
                if (SecurityContextHolder.getContext().getAuthentication() != null) {
                    System.out.println("   Existing auth: " + SecurityContextHolder.getContext().getAuthentication().getName());
                }
            }
        } catch (JwtException e) {
            System.out.println("❌ JWT ERROR: " + e.getMessage());
            e.printStackTrace();
            SecurityContextHolder.clearContext();
        } catch (Exception e) {
            System.out.println("❌ UNEXPECTED ERROR: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            SecurityContextHolder.clearContext();
        }
        
        System.out.println("========== END JWT FILTER ==========\n");
        chain.doFilter(req, res);
    }
}