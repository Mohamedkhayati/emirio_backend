package com.emirio.catalog;

import com.emirio.catalog.repo.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CategorySeeder implements CommandLineRunner {

  private final CategoryRepository categoryRepository;

  public CategorySeeder(CategoryRepository categoryRepository) {
    this.categoryRepository = categoryRepository;
  }

  @Override
  public void run(String... args) {
    seed("Men", "Products for men");
    seed("Women", "Products for women");
    seed("Kids", "Products for kids");
    seed("Accessories", "Accessories and essentials");
  }

  private void seed(String nom, String description) {
    boolean exists = categoryRepository.findAll()
      .stream()
      .anyMatch(c -> nom.equalsIgnoreCase(c.getNom()));

    if (!exists) {
      Category c = new Category();
      c.setNom(nom);
      c.setDescription(description);
      categoryRepository.save(c);
    }
  }
}
