package com.emirio.catalog;

import com.emirio.catalog.repo.SizeRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sizes")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class SizeController {

    private final SizeRepository repo;

    public SizeController(SizeRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<SizeDto> list() {
        return repo.findAllByOrderByPointureAsc().stream().map(SizeDto::from).toList();
    }

    public static class SizeDto {
        public Long id;
        public String pointure;

        static SizeDto from(Size s) {
            SizeDto d = new SizeDto();
            d.id = s.getId();
            d.pointure = s.getPointure();
            return d;
        }
    }
}