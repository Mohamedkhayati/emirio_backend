package com.emirio.chatbot.controller;

import com.emirio.chatbot.entity.ChatMessage;
import com.emirio.chatbot.repository.ChatMessageRepository;
import com.emirio.chatbot.service.ChatbotService;
import com.emirio.user.User;
import com.emirio.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> sendMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> payload) {
        String question = payload.get("question");
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        String answer = chatbotService.processQuestion(user, question);
        Map<String, String> response = new HashMap<>();
        response.put("answer", answer);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<ChatMessage>> getHistory(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<ChatMessage> history = chatMessageRepository.findByUserOrderByCreatedAtAsc(user);
        return ResponseEntity.ok(history);
    }
}