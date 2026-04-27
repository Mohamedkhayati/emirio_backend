// VisitController.java
package com.emirio.api;

import com.emirio.analytics.SiteVisit;
import com.emirio.analytics.SiteVisitRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/visit")
@RequiredArgsConstructor
public class VisitController {
    private final SiteVisitRepository siteVisitRepository;

    @PostMapping("/track")
    public void trackVisit(@RequestBody(required = false) Map<String, String> body,
                           HttpServletRequest request) {
        String sessionId = request.getSession(true).getId();
        String ip = request.getRemoteAddr();
        String pageUrl = body != null && body.containsKey("page") ? body.get("page") : request.getHeader("Referer");
        if (pageUrl == null) pageUrl = "direct";
        SiteVisit visit = new SiteVisit(sessionId, pageUrl, ip);
        siteVisitRepository.save(visit);
    }
}