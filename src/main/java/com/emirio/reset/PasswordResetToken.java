package com.emirio.reset;

import com.emirio.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name="mot_de_passe_reset")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PasswordResetToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true)
    private String code;

    @Column(nullable=false)
    private Instant dateExpiration;

    @ManyToOne(optional=false, fetch=FetchType.LAZY)
    private User user;
}
