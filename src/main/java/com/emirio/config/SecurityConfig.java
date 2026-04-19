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
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"));
        config.setAllowCredentials(true);

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

                .requestMatchers(HttpMethod.GET,
                    "/api/articles/**",
                    "/api/categories/**",
                    "/api/colors/**",
                    "/api/sizes/**"
                ).permitAll()

                // GET endpoints - Allow CONTROLEUR to read
                .requestMatchers(HttpMethod.GET,
                    "/api/admin/articles/**",
                    "/api/admin/categories/**",
                    "/api/admin/colors/**",
                    "/api/admin/sizes/**",
                    "/api/admin/variations/**"
                ).hasAnyRole("ADMIN_GENERAL", "VENDEUR", "CONTROLEUR")

                // POST/PUT/PATCH/DELETE - Only ADMIN_GENERAL and VENDEUR
                .requestMatchers(HttpMethod.POST,
                    "/api/admin/articles/**",
                    "/api/admin/categories/**",
                    "/api/admin/colors/**",
                    "/api/admin/sizes/**",
                    "/api/admin/variations/**"
                ).hasAnyRole("ADMIN_GENERAL", "VENDEUR")

                .requestMatchers(HttpMethod.PUT,
                    "/api/admin/articles/**",
                    "/api/admin/categories/**",
                    "/api/admin/colors/**",
                    "/api/admin/sizes/**",
                    "/api/admin/variations/**"
                ).hasAnyRole("ADMIN_GENERAL", "VENDEUR")

                .requestMatchers(HttpMethod.PATCH,
                    "/api/admin/articles/**",
                    "/api/admin/categories/**",
                    "/api/admin/colors/**",
                    "/api/admin/sizes/**",
                    "/api/admin/variations/**"
                ).hasAnyRole("ADMIN_GENERAL", "VENDEUR")

                .requestMatchers(HttpMethod.DELETE,
                    "/api/admin/articles/**",
                    "/api/admin/categories/**",
                    "/api/admin/colors/**",
                    "/api/admin/sizes/**",
                    "/api/admin/variations/**"
                ).hasAnyRole("ADMIN_GENERAL", "VENDEUR")

                // Users management - only ADMIN_GENERAL
                .requestMatchers(
                    "/api/admin/users/**",
                    "/api/admin/clients/**"
                ).hasRole("ADMIN_GENERAL")

                // Orders - ADMIN_GENERAL and CONTROLEUR
                .requestMatchers(
                    "/api/admin/orders/**"
                ).hasAnyRole("ADMIN_GENERAL", "CONTROLEUR")

                // Dashboard - only ADMIN_GENERAL
                .requestMatchers(
                    "/api/admin/dashboard/**"
                ).hasRole("ADMIN_GENERAL")

                // Admin root - allow all admin roles to see the admin panel
                .requestMatchers("/admin", "/admin/**").hasAnyRole("ADMIN_GENERAL", "VENDEUR", "CONTROLEUR")

                .anyRequest().authenticated()
            )
            .oauth2Login(oauth -> oauth.successHandler(oAuth2LoginSuccessHandler))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}