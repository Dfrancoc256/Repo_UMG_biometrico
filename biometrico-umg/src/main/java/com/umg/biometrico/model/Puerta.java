package com.umg.biometrico.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "puertas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Puerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instalacion_id")
    private Instalacion instalacion;

    @Column(length = 150)
    private String nombre;

    @Column(length = 50)
    private String nivel;

    @Column(name = "es_salon")
    private Boolean esSalon = false;

    @Column(length = 300)
    private String descripcion;
}
