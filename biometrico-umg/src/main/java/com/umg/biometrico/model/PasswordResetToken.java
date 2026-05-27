package com.umg.biometrico.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
@Data
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String token;

    private LocalDateTime fechaExpiracion;

    private boolean usado = false;

    @ManyToOne
    @JoinColumn(name = "persona_id")
    private Persona persona;
}