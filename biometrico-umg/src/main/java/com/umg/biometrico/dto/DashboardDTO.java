package com.umg.biometrico.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {
    private Long totalEstudiantes;
    private Long totalCatedraticos;
    private Long totalPersonal;
    private Long totalCursos;
    private Long ingresosHoy;
    private Long asistenciasHoy;
    private Long personasRestringidas;
    private Long totalPersonas;
}
