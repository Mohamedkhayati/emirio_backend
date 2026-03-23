package com.emirio.catalog;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(name = "couleur")
public class Color {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 80)
  private String nom;

  @Column(nullable = false, length = 7)
  private String codeHex; // example: #FFFFFF
}
