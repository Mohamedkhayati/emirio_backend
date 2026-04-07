package com.emirio.catalog.repo;

import com.emirio.catalog.VariationImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VariationImageRepository extends JpaRepository<VariationImage, Long> {
    Optional<VariationImage> findByIdAndVariationId(Long id, Long variationId);
}