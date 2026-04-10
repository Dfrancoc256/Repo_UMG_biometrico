package com.umg.biometrico.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "curso_estudiantes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CursoEstudiante {

    @Id

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id")
    private Curso curso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id")
    private Persona estudiante;

    @CreationTimestamp
    @Column(name = "fecha_inscripcion")
    private LocalDateTime fechaInscripcion;
}
