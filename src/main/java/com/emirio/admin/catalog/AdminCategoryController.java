package com.emirio.admin.catalog;

import com.emirio.catalog.Category;
import com.emirio.catalog.repo.CategoryRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/admin/categories")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AdminCategoryController {

  private final CategoryRepository repo;

  public AdminCategoryController(CategoryRepository repo) {
    this.repo = repo;
  }

  @GetMapping
  public List<CategoryDto> list() {
    return repo.findAllByOrderByNomAsc().stream().map(CategoryDto::from).toList();
  }

  @GetMapping("/{id}")
  public CategoryDto details(@PathVariable Long id) {
    return CategoryDto.from(findCategory(id));
  }

  @PostMapping
  public CategoryDto create(@RequestBody @Valid CreateReq req) {
    String nom = req.getNom().trim();
    if (repo.existsByNomIgnoreCase(nom)) {
      throw new ResponseStatusException(BAD_REQUEST, "Category name already exists");
    }

    Category c = new Category();
    c.setNom(nom);
    c.setDescription(trimToNull(req.getDescription()));
    return CategoryDto.from(repo.save(c));
  }

  @PutMapping("/{id}")
  public CategoryDto update(@PathVariable Long id, @RequestBody @Valid CreateReq req) {
    Category c = findCategory(id);
    String nom = req.getNom().trim();

    if (repo.existsByNomIgnoreCaseAndIdNot(nom, id)) {
      throw new ResponseStatusException(BAD_REQUEST, "Category name already exists");
    }

    c.setNom(nom);
    c.setDescription(trimToNull(req.getDescription()));
    return CategoryDto.from(repo.save(c));
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    Category c = findCategory(id);
    repo.delete(c);
  }

  private Category findCategory(Long id) {
    return repo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Category not found"));
  }

  private String trimToNull(String value) {
    if (value == null) return null;
    String v = value.trim();
    return v.isEmpty() ? null : v;
  }

  @Data
  public static class CreateReq {
    @NotBlank
    @Size(max = 120)
    private String nom;

    @Size(max = 1000)
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