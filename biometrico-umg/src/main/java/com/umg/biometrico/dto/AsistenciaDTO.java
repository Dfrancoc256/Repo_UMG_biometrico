package com.umg.biometrico.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsistenciaDTO {
    private Long estudianteId;
    private String nombreCompleto;
    private String correo;
    private String fotoRuta;
    private String seccion;
    private Boolean presente;
    private String horaRegistro;
}
