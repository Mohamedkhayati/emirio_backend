package com.emirio.admin;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "admin_audit_log")
@Getter
@Setter
@NoArgsConstructor
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AdminAction action;

    @Column(nullable = false)
    private Long targetUserId;

    @Column(nullable = false, length = 180)
    private String targetEmail;

    @Column(nullable = false, updatable = false, length = 180)
    private String actorEmail;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(length = 2000)
    private String details;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (actorEmail == null || actorEmail.isBlank()) {
            actorEmail = "SYSTEM";
        }
    }
}