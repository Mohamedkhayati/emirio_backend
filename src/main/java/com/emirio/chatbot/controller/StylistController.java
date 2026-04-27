package com.emirio.chatbot.controller;

import com.emirio.chatbot.service.StylistService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/stylist")
public class StylistController {

    private final StylistService stylistService;

    public StylistController(StylistService stylistService) {
        this.stylistService = stylistService;
    }

    @PostMapping("/advice")
    public ResponseEntity<Map<String, String>> getAdvice(@RequestBody Map<String, String> payload) {
        String question = payload.get("question");
        if (question == null || question.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("answer", "Posez-moi une question sur votre style !"));
        }
        String advice = stylistService.getStylingAdvice(question);
        return ResponseEntity.ok(Map.of("answer", advice));
    }
}