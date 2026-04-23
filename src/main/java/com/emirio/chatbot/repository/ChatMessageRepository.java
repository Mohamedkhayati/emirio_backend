package com.emirio.chatbot.repository;

import com.emirio.chatbot.entity.ChatMessage;
import com.emirio.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByUserOrderByCreatedAtAsc(User user);
}