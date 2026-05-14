package com.umg.biometrico.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "personas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String nombre;

    @Column(length = 100)
    private String apellido;

    @Column(length = 25)
    private String telefono;

    @Column(length = 200)
    private String correo;

    @Column(name = "foto_ruta", length = 400)
    private String fotoRuta;

    @Column(name = "tipo_persona", length = 30)
    private String tipoPersona;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rol_id")
    private Rol rol;

    @ManyToOne
    @JoinColumn(name = "carrera_id")
    private Carrera carrera;

    @Column(length = 200)
    private String seccion;

    @Column(name = "numero_carnet", length = 40, nullable = false)
    private String numeroCarnet;

    @Column(length = 200)
    private String contrasena;

    @Column(name = "encoding_facial", columnDefinition = "text")
    private String encodingFacial;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(nullable = false)
    private Boolean restringido = false;

    @Column(name = "motivo_restriccion", columnDefinition = "text")
    private String motivoRestriccion;

    @CreationTimestamp
    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }

}
