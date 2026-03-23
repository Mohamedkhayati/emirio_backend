package com.emirio.admin.catalog;

import com.emirio.catalog.Color;
import com.emirio.catalog.repo.ColorRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/colors")
public class AdminColorController {

  private final ColorRepository repo;

  public AdminColorController(ColorRepository repo) {
    this.repo = repo;
  }

  @GetMapping
  public List<ColorDto> list() {
    return repo.findAll().stream().map(ColorDto::from).toList();
  }

  @GetMapping("/{id}")
  public ColorDto details(@PathVariable Long id) {
    Color c = repo.findById(id).orElseThrow();
    return ColorDto.from(c);
  }

  @PostMapping
  public ColorDto create(@RequestBody CreateReq req) {
    Color c = new Color();
    c.setNom(req.getNom());
    c.setCodeHex(req.getCodeHex());
    return ColorDto.from(repo.save(c));
  }

  @PutMapping("/{id}")
  public ColorDto update(@PathVariable Long id, @RequestBody CreateReq req) {
    Color c = repo.findById(id).orElseThrow();
    c.setNom(req.getNom());
    c.setCodeHex(req.getCodeHex());
    return ColorDto.from(repo.save(c));
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    repo.deleteById(id);
  }

  @Data
  public static class CreateReq {
    @NotBlank
    private String nom;

    @NotBlank
    private String codeHex;
  }

  @Data
  public static class ColorDto {
    private Long id;
    private String nom;
    private String codeHex;

    static ColorDto from(Color c) {
      ColorDto d = new ColorDto();
      d.id = c.getId();
      d.nom = c.getNom();
      d.codeHex = c.getCodeHex();
      return d;
    }
  }
}
