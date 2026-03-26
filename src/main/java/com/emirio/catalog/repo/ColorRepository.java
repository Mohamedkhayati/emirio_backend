package com.emirio.catalog.repo;

import com.emirio.catalog.Color;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ColorRepository extends JpaRepository<Color, Long> {
  boolean existsByNomIgnoreCase(String nom);
  boolean existsByNomIgnoreCaseAndIdNot(String nom, Long id);
  boolean existsByCodeHexIgnoreCase(String codeHex);
  boolean existsByCodeHexIgnoreCaseAndIdNot(String codeHex, Long id);
  List<Color> findAllByOrderByNomAsc();
}