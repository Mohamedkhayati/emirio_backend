package com.emirio.admin.catalog;

import com.emirio.catalog.Size;
import com.emirio.catalog.repo.SizeRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/sizes")
public class AdminSizeController {

  private final SizeRepository repo;

  public AdminSizeController(SizeRepository repo) {
    this.repo = repo;
  }

  @GetMapping
  public List<SizeDto> list() {
    return repo.findAll().stream().map(SizeDto::from).toList();
  }

  @GetMapping("/{id}")
  public SizeDto details(@PathVariable Long id) {
    Size s = repo.findById(id).orElseThrow();
    return SizeDto.from(s);
  }

  @PostMapping
  public SizeDto create(@RequestBody CreateReq req) {
    Size s = new Size();
    s.setPointure(req.getPointure());
    return SizeDto.from(repo.save(s));
  }

  @PutMapping("/{id}")
  public SizeDto update(@PathVariable Long id, @RequestBody CreateReq req) {
    Size s = repo.findById(id).orElseThrow();
    s.setPointure(req.getPointure());
    return SizeDto.from(repo.save(s));
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    repo.deleteById(id);
  }

  @Data
  public static class CreateReq {
    @NotBlank
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
