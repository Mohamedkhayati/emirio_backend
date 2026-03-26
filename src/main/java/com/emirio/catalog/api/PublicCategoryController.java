package com.emirio.catalog.api;

import com.emirio.catalog.Category;
import com.emirio.catalog.repo.CategoryRepository;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class PublicCategoryController {

  private final CategoryRepository repo;

  public PublicCategoryController(CategoryRepository repo) {
    this.repo = repo;
  }

  @GetMapping
  public List<CategoryDto> list() {
    return repo.findAll().stream()
      .sorted(Comparator.comparing(Category::getNom, String.CASE_INSENSITIVE_ORDER))
      .map(CategoryDto::from)
      .toList();
  }

  @Data
  public static class CategoryDto {
    private Long id;
    private String nom;
    private String description;

    static CategoryDto from(Category c) {
      CategoryDto d = new CategoryDto();
      d.id = c.getId();
      d.nom = c.getNom();
      d.description = c.getDescription();
      return d;
    }
  }
}