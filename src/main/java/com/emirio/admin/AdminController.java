package com.emirio.admin;

import com.emirio.user.Role;
import com.emirio.user.User;
import com.emirio.user.UserRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

  private final UserRepository users;

  public AdminController(UserRepository users) {
    this.users = users;
  }

  @GetMapping("/clients")
  public List<ClientDto> listClients() {
    return users.findAll().stream()
      .filter(u -> u.getRole() == Role.CLIENT)
      .map(ClientDto::from)
      .toList();
  }

  @GetMapping("/clients/{id}")
  public ClientDto getClient(@PathVariable Long id) {
    User u = users.findById(id).orElseThrow();
    return ClientDto.from(u);
  }
  @DeleteMapping("/clients/{id}")
  public void deleteClient(@PathVariable Long id) {
    users.deleteById(id);
  }


  @PutMapping("/clients/{id}/status")
  public ClientDto updateStatus(@PathVariable Long id, @RequestBody StatusReq req) {
    User u = users.findById(id).orElseThrow();
    u.setStatutCompte(req.getStatutCompte()); // "ACTIVE" / "BLOCKED"
    users.save(u);
    return ClientDto.from(u);
  }

  @Data
  public static class StatusReq {
    @NotBlank
    private String statutCompte;
  }

  @Data
  public static class ClientDto {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String role;
    private String statutCompte;

    public static ClientDto from(User u) {
      ClientDto dto = new ClientDto();
      dto.id = u.getId();
      dto.nom = u.getNom();
      dto.prenom = u.getPrenom();
      dto.email = u.getEmail();
      dto.role = u.getRole().name();
      dto.statutCompte = u.getStatutCompte();
      return dto;
    }
  }
}
