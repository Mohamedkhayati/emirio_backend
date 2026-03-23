package com.emirio.catalog;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(
  name = "variation_article",
  uniqueConstraints = @UniqueConstraint(
    name = "uk_article_color_size",
    columnNames = {"article_id", "couleur_id", "taille_id"}
  )
)
public class VariationArticle {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private int quantiteStock;

  @Column(nullable = false)
  private double prix;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "article_id")
  private Article article;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "couleur_id")
  private Color couleur;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "taille_id")
  private Size taille;
}
