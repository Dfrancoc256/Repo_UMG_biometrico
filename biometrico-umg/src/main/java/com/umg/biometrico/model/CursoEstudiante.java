package com.umg.biometrico.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "curso_estudiantes")
@IdClass(CursoEstudianteId.class)
@Data
@NoArgsConstructor
public class CursoEstudiante {

    @Id
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id")
    private Curso curso;

    @Id
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "estudiante_id")
    private Persona estudiante;

    @CreationTimestamp
    @Column(name = "fecha_inscripcion")
    private LocalDateTime fechaInscripcion;
}
