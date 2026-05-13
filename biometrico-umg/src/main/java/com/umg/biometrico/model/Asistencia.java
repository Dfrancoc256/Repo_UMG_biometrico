package com.umg.biometrico.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "asistencia")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Asistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id")
    private Persona estudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id")
    private Curso curso;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private Boolean presente = false;

    @Column(name = "hora_registro")
    private LocalDateTime horaRegistro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sesion_asistencia_id")
    private SesionAsistencia sesionAsistencia;

    @Column(name = "metodo_registro", length = 30)
    private String metodoRegistro;

    @Column(name = "similitud_facial")
    private Double similitudFacial;
}
