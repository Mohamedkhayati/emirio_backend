package com.emirio.admin.catalog;

import com.emirio.catalog.Size;
import com.emirio.catalog.repo.SizeRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/admin/sizes")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AdminSizeController {

  private final SizeRepository repo;

  public AdminSizeController(SizeRepository repo) {
    this.repo = repo;
  }

  @GetMapping
  public List<SizeDto> list() {
    return repo.findAllByOrderByPointureAsc().stream().map(SizeDto::from).toList();
  }

  @GetMapping("/{id}")
  public SizeDto details(@PathVariable Long id) {
    return SizeDto.from(findSize(id));
  }

  @PostMapping
  public SizeDto create(@RequestBody @Valid CreateReq req) {
    String pointure = req.getPointure().trim();

    if (repo.existsByPointureIgnoreCase(pointure)) {
      throw new ResponseStatusException(BAD_REQUEST, "Size already exists");
    }

    Size s = new Size();
    s.setPointure(pointure);
    return SizeDto.from(repo.save(s));
  }

  @PutMapping("/{id}")
  public SizeDto update(@PathVariable Long id, @RequestBody @Valid CreateReq req) {
    Size s = findSize(id);
    String pointure = req.getPointure().trim();

    if (repo.existsByPointureIgnoreCaseAndIdNot(pointure, id)) {
      throw new ResponseStatusException(BAD_REQUEST, "Size already exists");
    }

    s.setPointure(pointure);
    return SizeDto.from(repo.save(s));
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    Size s = findSize(id);
    repo.delete(s);
  }

  private Size findSize(Long id) {
    return repo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Size not found"));
  }

  @Data
  public static class CreateReq {
    @NotBlank
    @jakarta.validation.constraints.Size(max = 40)
    private String pointure;
  }

  @Data
  public static class SizeDto {
    private Long id;
    private String pointure;

    static SizeDto from(Size s) {
      SizeDto d = new SizeDto();
      d.id = s.getId();
      d.pointure = s.getPointure();
      return d;
    }
  }
}