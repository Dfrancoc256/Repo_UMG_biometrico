package com.umg.biometrico.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Entity
@Table(name = "instalaciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Instalacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 200)
    private String nombre;

    @Column(length = 300)
    private String direccion;

    @OneToMany(mappedBy = "instalacion", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Puerta> puertas;
}
