package com.umg.biometrico.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "curso_seccion_asignacion",
       uniqueConstraints = @UniqueConstraint(columnNames = {"curso_id", "seccion"}))
@Getter @Setter @NoArgsConstructor
public class CursoSeccionAsignacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @Column(nullable = false, length = 1)
    private String seccion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "catedratico_id", nullable = false)
    private Persona catedratico;
}
