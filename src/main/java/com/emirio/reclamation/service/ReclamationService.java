package com.emirio.reclamation.service;

import com.emirio.reclamation.dto.*;
import com.emirio.reclamation.entity.Reclamation;
import com.emirio.reclamation.entity.ReclamationHistory;
import com.emirio.reclamation.entity.ReclamationMessage;
import com.emirio.reclamation.repository.ReclamationHistoryRepository;
import com.emirio.reclamation.repository.ReclamationMessageRepository;
import com.emirio.reclamation.repository.ReclamationRepository;
import com.emirio.user.User;
import com.emirio.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReclamationService {

    private final ReclamationRepository reclamationRepository;
    private final ReclamationMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final ReclamationHistoryRepository historyRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private void addHistory(Reclamation reclamation, User actor, String action, String oldVal, String newVal, String details) {
        ReclamationHistory history = ReclamationHistory.builder()
                .reclamation(reclamation)
                .actor(actor)
                .action(action)
                .oldValue(oldVal)
                .newValue(newVal)
                .details(details)
                .build();
        historyRepository.save(history);
    }

    @Transactional
    public ReclamationResponse createReclamation(ReclamationRequest request) {
        User client = getCurrentUser();
        Reclamation reclamation = Reclamation.builder()
                .user(client)
                .subject(request.getSubject())
                .description(request.getDescription())
                .status("OPEN")
                .build();
        reclamation = reclamationRepository.save(reclamation);

        ReclamationMessage firstMsg = ReclamationMessage.builder()
                .reclamation(reclamation)
                .sender(client)
                .content(request.getDescription())
                .build();
        firstMsg = messageRepository.save(firstMsg);
        reclamation.getMessages().add(firstMsg);

        // History entry
        addHistory(reclamation, client, "CREATED", null, null,
                "Reclamation created with subject: " + request.getSubject());

        return ReclamationResponse.from(reclamation);
    }

    public List<ReclamationResponse> getAllReclamations() {
        return reclamationRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(ReclamationResponse::from)
                .collect(Collectors.toList());
    }

    public Reclamation getReclamationByIdEntity(Long id) {
        return reclamationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reclamation not found"));
    }

    public ReclamationResponse getReclamationById(Long id) {
        Reclamation reclamation = getReclamationByIdEntity(id);
        List<ReclamationHistory> history = historyRepository.findByReclamationIdOrderByCreatedAtAsc(id);
        return ReclamationResponse.withHistory(reclamation, history);
    }

    public List<ReclamationResponse> getMyReclamations() {
        User client = getCurrentUser();
        return reclamationRepository.findByUserOrderByCreatedAtDesc(client).stream()
                .map(ReclamationResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public MessageResponse addReply(Long reclamationId, MessageRequest request) {
        User admin = getCurrentUser();
        String role = admin.getRole().getName();
        if (!role.equals("Administrateur") && !role.equals("Responsable e-commerce")) {
            throw new AccessDeniedException("Only Administrateur or Responsable e-commerce can reply");
        }

        Reclamation reclamation = getReclamationByIdEntity(reclamationId);
        if ("OPEN".equals(reclamation.getStatus())) {
            reclamation.setStatus("IN_PROGRESS");
        }

        ReclamationMessage reply = ReclamationMessage.builder()
                .reclamation(reclamation)
                .sender(admin)
                .content(request.getContent())
                .build();
        reply = messageRepository.save(reply);
        reclamation.getMessages().add(reply);
        reclamationRepository.save(reclamation);

        emailService.sendReclamationReply(
                reclamation.getUser().getEmail(),
                reclamation.getSubject(),
                request.getContent(),
                String.valueOf(reclamation.getId())
        );

        addHistory(reclamation, admin, "MESSAGE_ADDED", null, null,
                "Admin replied: " + (request.getContent().length() > 50 ?
                        request.getContent().substring(0, 50) + "..." : request.getContent()));

        return MessageResponse.from(reply);
    }

    @Transactional
    public ReclamationResponse updateStatus(Long id, String status) {
        Reclamation reclamation = getReclamationByIdEntity(id);
        String oldStatus = reclamation.getStatus();

        if (!List.of("OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED").contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
        }
        reclamation.setStatus(status);
        reclamation = reclamationRepository.save(reclamation);

        addHistory(reclamation, getCurrentUser(), "STATUS_CHANGED", oldStatus, status, null);

        return ReclamationResponse.from(reclamation);
    }

    @Transactional
    public MessageResponse addClientMessage(Long reclamationId, MessageRequest request) {
        User client = getCurrentUser();
        Reclamation reclamation = getReclamationByIdEntity(reclamationId);

        if (!reclamation.getUser().getId().equals(client.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only reply to your own reclamations");
        }
        if ("CLOSED".equals(reclamation.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This reclamation is closed and cannot be replied to");
        }

        ReclamationMessage msg = ReclamationMessage.builder()
                .reclamation(reclamation)
                .sender(client)
                .content(request.getContent())
                .build();
        msg = messageRepository.save(msg);
        reclamation.getMessages().add(msg);
        if ("OPEN".equals(reclamation.getStatus())) {
            reclamation.setStatus("IN_PROGRESS");
        }
        reclamationRepository.save(reclamation);

        addHistory(reclamation, client, "MESSAGE_ADDED", null, null,
                "Client replied: " + (request.getContent().length() > 50 ?
                        request.getContent().substring(0, 50) + "..." : request.getContent()));

        return MessageResponse.from(msg);
    }
 // Add this method to ReclamationService.java

    public List<HistoryEntryResponse> getReclamationHistory(Long reclamationId) {
        // First check if reclamation exists
        getReclamationByIdEntity(reclamationId);
        List<ReclamationHistory> historyList = historyRepository.findByReclamationIdOrderByCreatedAtAsc(reclamationId);
        return historyList.stream()
                .map(HistoryEntryResponse::from)
                .collect(Collectors.toList());
    }
}