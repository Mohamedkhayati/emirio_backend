package com.emirio.admin.user;

import com.emirio.user.User;
import com.emirio.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequiredArgsConstructor
public class AdminUserMediaController {

    private final UserRepository users;

    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> photo(@PathVariable Long id) {
        User user = users.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));

        if (user.getPhotoData() == null || user.getPhotoData().length == 0) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            if (user.getPhotoType() != null && !user.getPhotoType().isBlank()) {
                mediaType = MediaType.parseMediaType(user.getPhotoType());
            }
        } catch (Exception ignored) {}

        return ResponseEntity.ok()
            .contentType(mediaType)
            .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0")
            .body(user.getPhotoData());
    }
}