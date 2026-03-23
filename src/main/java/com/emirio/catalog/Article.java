package com.emirio.catalog;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "article")
public class Article {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

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

  @Column(length = 160)
  private String marque;

  @Column(length = 160)
  private String matiere;

  @Column(length = 120, unique = true)
  private String sku;

  @Column(name = "sale_price")
  private Double salePrice;

  @Column(name = "sale_start_at")
  private LocalDateTime saleStartAt;

  @Column(name = "sale_end_at")
  private LocalDateTime saleEndAt;

  @Column(name = "recommended")
  private boolean recommended = false;

  @Lob
  @Basic(fetch = FetchType.LAZY)
  @Column(name = "image_data1", columnDefinition = "LONGBLOB")
  private byte[] imageData1;

  @Column(name = "image_name1")
  private String imageName1;

  @Column(name = "image_type1")
  private String imageType1;

  @Lob
  @Basic(fetch = FetchType.LAZY)
  @Column(name = "image_data2", columnDefinition = "LONGBLOB")
  private byte[] imageData2;

  @Column(name = "image_name2")
  private String imageName2;

  @Column(name = "image_type2")
  private String imageType2;

  @Lob
  @Basic(fetch = FetchType.LAZY)
  @Column(name = "image_data3", columnDefinition = "LONGBLOB")
  private byte[] imageData3;

  @Column(name = "image_name3")
  private String imageName3;

  @Column(name = "image_type3")
  private String imageType3;

  @Lob
  @Basic(fetch = FetchType.LAZY)
  @Column(name = "image_data4", columnDefinition = "LONGBLOB")
  private byte[] imageData4;

  @Column(name = "image_name4")
  private String imageName4;

  @Column(name = "image_type4")
  private String imageType4;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "categorie_id")
  private Category categorie;
}
