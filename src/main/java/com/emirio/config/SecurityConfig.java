package com.emirio.config;

import com.emirio.security.JwtAuthFilter;
import com.emirio.security.OAuth2LoginSuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity(debug = true)
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    
    private final JwtAuthFilter jwtAuthFilter;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        
        // Add ALL origins that your Flutter web might use
        config.setAllowedOrigins(List.of(
            "http://localhost:5173",     // React/Vite default
            "http://127.0.0.1:5173",
            "http://localhost:3000",      // React default
            "http://localhost:5000",      // Flutter web default
            "http://localhost:56112",     // Your current Flutter port
            "http://localhost:8080",      // Backend itself
            "http://127.0.0.1:5000",
            "http://127.0.0.1:56112",
            "http://localhost:53454",     // Common Flutter random port
            "http://localhost:53500",     // Common Flutter random port
            "http://localhost:*",         // Any localhost port (if supported)
            "http://192.168.1.*:*"        // Network access
        ));
        
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        config.setAllowedHeaders(List.of(
            "Authorization", 
            "Content-Type", 
            "X-Requested-With", 
            "Accept", 
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        config.setExposedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .cors(withDefaults())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .requestCache(cache -> cache.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    logger.error("Unauthorized error. URI: {}, AuthException: {}", request.getRequestURI(), authException.getMessage());
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    logger.error("Forbidden error. URI: {}, AccessDeniedException: {}", request.getRequestURI(), accessDeniedException.getMessage());
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
                })
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                .requestMatchers(HttpMethod.GET, "/api/catalog/variations/*/images/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/catalog/variations/*/model").permitAll()

                .requestMatchers(
                    "/api/auth/**",
                    "/oauth2/**",
                    "/login/**",
                    "/error",
                    "/favicon.ico"
                ).permitAll()
                .requestMatchers("/api/debug/**").permitAll()

                .requestMatchers(HttpMethod.GET,
                    "/api/articles/**",
                    "/api/categories/**",
                    "/api/colors/**",
                    "/api/sizes/**"
                ).permitAll()

                // Customers endpoints - Only Administrateur
                .requestMatchers(
                    "/api/admin/clients", "/api/admin/clients/**",
                    "/api/admin/users", "/api/admin/users/**"
                ).hasAuthority("Administrateur")

                // Workers endpoints - Only Administrateur
                .requestMatchers(
                    "/api/admin/workers", "/api/admin/workers/**"
                ).hasAuthority("Administrateur")

                // Catalog endpoints - Administrateur, Gestionnaire de catalogue, AND Responsable e-commerce
                .requestMatchers(
                    "/api/admin/articles", "/api/admin/articles/**",
                    "/api/admin/categories", "/api/admin/categories/**",
                    "/api/admin/colors", "/api/admin/colors/**",
                    "/api/admin/sizes", "/api/admin/sizes/**",
                    "/api/admin/variations", "/api/admin/variations/**"
                ).hasAnyAuthority("Administrateur", "Gestionnaire de catalogue", "Responsable e-commerce")
                
                // Seller (Vendeur) endpoints
                .requestMatchers(
                    "/api/vendeur/**"
                ).hasAnyAuthority("Gestionnaire de catalogue", "Administrateur")

                // Dashboard endpoints - Only Administrateur
                .requestMatchers(
                    "/api/admin/dashboard", "/api/admin/dashboard/**",
                    "/api/admin/recommendation-config"
                ).hasAuthority("Administrateur")

                // Orders endpoints - Administrateur and Responsable e-commerce
                .requestMatchers(
                    "/api/admin/orders", "/api/admin/orders/**"
                ).hasAnyAuthority("Administrateur", "Responsable e-commerce")

                // RECLAMATION ENDPOINTS
                .requestMatchers(HttpMethod.POST, "/api/reclamations").hasAuthority("Client")
                .requestMatchers(HttpMethod.GET, "/api/reclamations/my").hasAuthority("Client")
                .requestMatchers(HttpMethod.POST, "/api/reclamations/*/client-messages").hasAuthority("Client")
                .requestMatchers(HttpMethod.GET, "/api/reclamations/*").authenticated()
                .requestMatchers("/api/reclamations/**").hasAnyAuthority("Administrateur", "Responsable e-commerce")
                .requestMatchers("/api/chat/send", "/api/chat/history").authenticated()
                
                // Admin root
                .requestMatchers("/admin", "/admin/**").hasAnyAuthority(
                    "Administrateur", 
                    "Gestionnaire de catalogue", 
                    "Responsable e-commerce"
                )

                .anyRequest().authenticated()
            )
            .oauth2Login(oauth -> oauth.successHandler(oAuth2LoginSuccessHandler))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}