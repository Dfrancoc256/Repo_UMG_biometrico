package com.umg.biometrico.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "camaras")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Camara {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 150, nullable = false)
    private String nombre;

    @Column(length = 100)
    private String ip;

    @Column(length = 200)
    private String token;

    @Column(nullable = false)
    private Boolean activa = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "puerta_id")
    private Puerta puerta;

    @Column(length = 30)
    private String modo;

    @Column(name = "ultima_conexion")
    private LocalDateTime ultimaConexion;
}