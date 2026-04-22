package com.emirio.reclamation.controller;

import com.emirio.reclamation.dto.HistoryEntryResponse;
import com.emirio.reclamation.dto.MessageRequest;
import com.emirio.reclamation.dto.MessageResponse;
import com.emirio.reclamation.dto.ReclamationRequest;
import com.emirio.reclamation.dto.ReclamationResponse;
import com.emirio.reclamation.entity.Reclamation;
import com.emirio.reclamation.service.ReclamationService;
import com.emirio.user.User;
import com.emirio.user.UserRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/reclamations")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class ReclamationController {

    private final ReclamationService reclamationService;
    private final UserRepository userRepository;  // needed for getCurrentUser

    // Helper to get current user from Authentication
    private User getCurrentUser(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    // Client endpoints
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('Client')")
    public ReclamationResponse createReclamation(@Valid @RequestBody ReclamationRequest request) {
        return reclamationService.createReclamation(request);
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('Client')")
    public List<ReclamationResponse> getMyReclamations() {
        return reclamationService.getMyReclamations();
    }

    // Admin / Responsable e-commerce endpoints
    @GetMapping
    @PreAuthorize("hasAnyAuthority('Administrateur', 'Responsable e-commerce')")
    public List<ReclamationResponse> getAllReclamations() {
        return reclamationService.getAllReclamations();
    }

    // Endpoint accessible by both admin and the owner client
    @GetMapping("/{id}")
    public ReclamationResponse getReclamationById(@PathVariable Long id, Authentication auth) {
        Reclamation reclamation = reclamationService.getReclamationByIdEntity(id);
        User currentUser = getCurrentUser(auth);
        
        String role = currentUser.getRole().getName();
        boolean isAdmin = "Administrateur".equals(role) || "Responsable e-commerce".equals(role);
        boolean isOwner = reclamation.getUser().getId().equals(currentUser.getId());
        
        if (!isAdmin && !isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view your own reclamations");
        }
        return ReclamationResponse.from(reclamation);
    }

    @PostMapping("/{id}/messages")
    @PreAuthorize("hasAnyAuthority('Administrateur', 'Responsable e-commerce')")
    public MessageResponse addReply(@PathVariable Long id, @Valid @RequestBody MessageRequest request) {
        return reclamationService.addReply(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('Administrateur', 'Responsable e-commerce')")
    public ReclamationResponse updateStatus(@PathVariable Long id, @RequestParam String status) {
        return reclamationService.updateStatus(id, status);
    }

    @PostMapping("/{id}/client-messages")
    @PreAuthorize("hasAuthority('Client')")
    public MessageResponse addClientMessage(@PathVariable Long id, @Valid @RequestBody MessageRequest request) {
        return reclamationService.addClientMessage(id, request);
    }
 // Add this method to ReclamationController.java

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyAuthority('Administrateur', 'Responsable e-commerce')")
    public List<HistoryEntryResponse> getReclamationHistory(@PathVariable Long id) {
        return reclamationService.getReclamationHistory(id);
    }
}