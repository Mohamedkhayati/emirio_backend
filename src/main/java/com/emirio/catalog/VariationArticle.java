package com.emirio.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "variation_article")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class VariationArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "couleur_id", nullable = false, 
                foreignKey = @ForeignKey(name = "FK_VARIATION_ARTICLE_COULEUR"))
    private Color couleur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taille_id", 
                foreignKey = @ForeignKey(name = "FK_VARIATION_ARTICLE_TAILLE"))
    private Size taille;

    @Column(name = "quantite_stock", nullable = false)
    private int quantiteStock;

    @Column(nullable = false)
    private double prix;

    // KEEP THESE - Many classes still reference them
    @JsonIgnore
    @Lob
    @Column(name = "model_3d_data", columnDefinition = "LONGBLOB")
    private byte[] model3dData;

    @Column(name = "model_3d_name")
    private String model3dName;

    @Column(name = "model_3d_type")
    private String model3dType;

    // ADD THESE for file-based storage
    @Column(name = "model_3d_file_path")
    private String model3dFilePath;
    
    @Column(name = "model_3d_file_name")
    private String model3dFileName;
    
    @Column(name = "model_3d_file_size")
    private Long model3dFileSize;
    
    @Column(name = "model_3d_content_type")
    private String model3dContentType;

    @OneToMany(mappedBy = "variation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<VariationImage> images = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by")
    private String createdBy;

    @LastModifiedBy
    @Column(name = "last_modified_by")
    private String lastModifiedBy;

    @Column(nullable = false)
    private boolean ruptureStockNotifEnvoyee = false;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public String getModel3dUrl() {
        // Check both file path and data
        if (model3dFilePath != null && !model3dFilePath.isEmpty()) {
            return "/api/variations/" + id + "/model";
        }
        if (model3dData != null && model3dData.length > 0) {
            return "/api/variations/" + id + "/model";
        }
        return null;
    }
}