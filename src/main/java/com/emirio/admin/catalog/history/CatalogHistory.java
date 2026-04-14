package com.emirio.admin.catalog.history;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "catalog_history", indexes = {
    @Index(name = "idx_catalog_history_article", columnList = "article_id, action_at"),
    @Index(name = "idx_catalog_history_variation", columnList = "variation_id, action_at"),
    @Index(name = "idx_catalog_history_target", columnList = "target_type, target_id, action_at")
})
@Getter
@Setter
public class CatalogHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private CatalogTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "article_id")
    private Long articleId;

    @Column(name = "variation_id")
    private Long variationId;

    @Column(name = "article_name", length = 180)
    private String articleName;

    @Column(name = "variation_label", length = 255)
    private String variationLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CatalogAction action;

    @Column(name = "action_at", nullable = false, updatable = false)
    private Instant actionAt;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_full_name", length = 180)
    private String actorFullName;

    @Column(name = "actor_email", length = 180)
    private String actorEmail;

    @Column(name = "summary", length = 500)
    private String summary;

    @Lob
    @Column(name = "details_json", columnDefinition = "LONGTEXT")
    private String detailsJson;

    @PrePersist
    void onCreate() {
        if (actionAt == null) actionAt = Instant.now();
    }
}