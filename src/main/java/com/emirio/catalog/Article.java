package com.emirio.catalog;

import com.emirio.user.User;
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

@Entity
@Table(name = "article")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false, length = 180)
    private String nom;

    @Column(length = 2000)
    private String description;

    @Column(length = 6000)
    private String details;

    @Column(nullable = false)
    private double prix;

    @Column(nullable = false)
    private boolean actif = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categorie_id", nullable = false)
    private Category categorie;

    @Column(length = 160)
    private String marque;

    @Column(length = 160)
    private String matiere;

    @Column(length = 120, unique = true)
    private String sku;

    @JsonIgnore
    @Lob
    @Column(name = "image_data1")
    private byte[] imageData1;

    @Column(name = "image_name1")
    private String imageName1;

    @Column(name = "image_type1")
    private String imageType1;

    @JsonIgnore
    @Lob
    @Column(name = "image_data2")
    private byte[] imageData2;

    @Column(name = "image_name2")
    private String imageName2;

    @Column(name = "image_type2")
    private String imageType2;

    @JsonIgnore
    @Lob
    @Column(name = "image_data3")
    private byte[] imageData3;

    @Column(name = "image_name3")
    private String imageName3;

    @Column(name = "image_type3")
    private String imageType3;

    @JsonIgnore
    @Lob
    @Column(name = "image_data4")
    private byte[] imageData4;

    @Column(name = "image_name4")
    private String imageName4;

    @Column(name = "image_type4")
    private String imageType4;

    @Column(name = "sale_price")
    private Double salePrice;

    @Column(name = "sale_start_at")
    private LocalDateTime saleStartAt;

    @Column(name = "sale_end_at")
    private LocalDateTime saleEndAt;

    @Column(nullable = false)
    private boolean recommended = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendeur_id")
    private User vendeur;

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
    
    // Helper methods to get image URLs (not the data)
    public String getImageUrl1() {
        return imageData1 != null ? "/api/articles/" + id + "/image/1" : null;
    }
    
    public String getImageUrl2() {
        return imageData2 != null ? "/api/articles/" + id + "/image/2" : null;
    }
    
    public String getImageUrl3() {
        return imageData3 != null ? "/api/articles/" + id + "/image/3" : null;
    }
    
    public String getImageUrl4() {
        return imageData4 != null ? "/api/articles/" + id + "/image/4" : null;
    }
}