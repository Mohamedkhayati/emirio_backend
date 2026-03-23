package com.emirio.admin.catalog;

import com.emirio.catalog.Category;
import com.emirio.catalog.repo.CategoryRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {
  private final CategoryRepository repo;

  public AdminCategoryController(CategoryRepository repo) {
    this.repo = repo;
  }

  @GetMapping
  public List<CategoryDto> list() {
    return repo.findAll().stream().map(CategoryDto::from).toList();
  }

  @PostMapping
  public CategoryDto create(@RequestBody CreateReq req) {
    Category c = new Category();
    c.setNom(req.getNom());
    c.setDescription(req.getDescription());
    return CategoryDto.from(repo.save(c));
  }

  @PutMapping("/{id}")
  public CategoryDto update(@PathVariable Long id, @RequestBody CreateReq req) {
    Category c = repo.findById(id).orElseThrow();
    c.setNom(req.getNom());
    c.setDescription(req.getDescription());
    return CategoryDto.from(repo.save(c));
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    repo.deleteById(id);
  }

  @Data
  public static class CreateReq {
    @NotBlank private String nom;
    private String description;
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
