package com.emirio.catalog;

import com.emirio.catalog.repo.ColorRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/colors")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class ColorController {

    private final ColorRepository repo;

    public ColorController(ColorRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<ColorDto> list() {
        return repo.findAllByOrderByNomAsc().stream().map(ColorDto::from).toList();
    }

    public static class ColorDto {
        public Long id;
        public String nom;
        public String codeHex;

        static ColorDto from(Color c) {
            ColorDto d = new ColorDto();
            d.id = c.getId();
            d.nom = c.getNom();
            d.codeHex = c.getCodeHex();
            return d;
        }
    }
}