package com.emirio.catalog.repo;

import com.emirio.catalog.Size;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SizeRepository extends JpaRepository<Size, Long> {
  boolean existsByPointureIgnoreCase(String pointure);
  boolean existsByPointureIgnoreCaseAndIdNot(String pointure, Long id);
  List<Size> findAllByOrderByPointureAsc();
}