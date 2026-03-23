package com.emirio.catalog.repo;

import com.emirio.catalog.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {}
