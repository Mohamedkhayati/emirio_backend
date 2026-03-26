package com.emirio.admin.catalog;

import com.emirio.catalog.Color;
import com.emirio.catalog.repo.ColorRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/admin/colors")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AdminColorController {

  private final ColorRepository repo;

  public AdminColorController(ColorRepository repo) {
    this.repo = repo;
  }

  @GetMapping
  public List<ColorDto> list() {
    return repo.findAllByOrderByNomAsc().stream().map(ColorDto::from).toList();
  }

  @GetMapping("/{id}")
  public ColorDto details(@PathVariable Long id) {
    return ColorDto.from(findColor(id));
  }

  @PostMapping
  public ColorDto create(@RequestBody @Valid CreateReq req) {
    String nom = req.getNom().trim();
    String codeHex = req.getCodeHex().trim().toUpperCase();

    if (repo.existsByNomIgnoreCase(nom)) {
      throw new ResponseStatusException(BAD_REQUEST, "Color name already exists");
    }

    if (repo.existsByCodeHexIgnoreCase(codeHex)) {
      throw new ResponseStatusException(BAD_REQUEST, "Color hex already exists");
    }

    Color c = new Color();
    c.setNom(nom);
    c.setCodeHex(codeHex);
    return ColorDto.from(repo.save(c));
  }

  @PutMapping("/{id}")
  public ColorDto update(@PathVariable Long id, @RequestBody @Valid CreateReq req) {
    Color c = findColor(id);
    String nom = req.getNom().trim();
    String codeHex = req.getCodeHex().trim().toUpperCase();

    if (repo.existsByNomIgnoreCaseAndIdNot(nom, id)) {
      throw new ResponseStatusException(BAD_REQUEST, "Color name already exists");
    }

    if (repo.existsByCodeHexIgnoreCaseAndIdNot(codeHex, id)) {
      throw new ResponseStatusException(BAD_REQUEST, "Color hex already exists");
    }

    c.setNom(nom);
    c.setCodeHex(codeHex);
    return ColorDto.from(repo.save(c));
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    Color c = findColor(id);
    repo.delete(c);
  }

  private Color findColor(Long id) {
    return repo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Color not found"));
  }

  @Data
  public static class CreateReq {
    @NotBlank
    @Size(max = 80)
    private String nom;

    @NotBlank
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$")
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