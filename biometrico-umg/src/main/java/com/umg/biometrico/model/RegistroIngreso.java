package com.umg.biometrico.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "registro_ingreso")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroIngreso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id")
    private Persona persona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "puerta_id")
    private Puerta puerta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id")
    private Curso curso;

    @Column(name = "fecha_hora")
    private LocalDateTime fechaHora;

    @Column(length = 20)
    private String metodo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "camara_id")
    private Camara camara;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sesion_asistencia_id")
    private SesionAsistencia sesionAsistencia;

    @Column(name = "similitud_facial")
    private Double similitudFacial;

    @Column(name = "acceso_permitido")
    private Boolean accesoPermitido = true;

    @Column(name = "metodo_ingreso", length = 20)
    private String metodoIngreso;

    @Column(columnDefinition = "text")
    private String observaciones;
}