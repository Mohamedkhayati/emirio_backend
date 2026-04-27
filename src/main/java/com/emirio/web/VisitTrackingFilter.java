package com.emirio.web;

import com.emirio.analytics.SiteVisit;
import com.emirio.analytics.SiteVisitRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class VisitTrackingFilter extends OncePerRequestFilter {

    private final SiteVisitRepository siteVisitRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if ("GET".equals(request.getMethod()) && 
            !uri.startsWith("/api") && 
            !uri.startsWith("/assets") &&
            !uri.startsWith("/css") &&
            !uri.startsWith("/js")) {
            
            String sessionId = request.getSession(true).getId();
            String ip = request.getRemoteAddr();
            SiteVisit visit = new SiteVisit(sessionId, uri, ip);
            siteVisitRepository.save(visit);
        }
        filterChain.doFilter(request, response);
    }
}