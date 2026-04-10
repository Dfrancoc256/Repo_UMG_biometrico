package com.umg.biometrico.service;

import com.umg.biometrico.dto.DashboardDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PersonaService personaService;
    private final CursoService cursoService;
    private final AsistenciaService asistenciaService;
    private final RegistroIngresoService registroIngresoService;

    public DashboardDTO obtenerEstadisticas() {
        DashboardDTO dto = new DashboardDTO();
        dto.setTotalEstudiantes(personaService.contarPorTipo("estudiante"));
        dto.setTotalCatedraticos(personaService.contarPorTipo("catedratico"));
        dto.setTotalPersonal(personaService.contarPorTipo("mantenimiento"));
        dto.setTotalCursos(cursoService.contarActivos());
        dto.setIngresosHoy(registroIngresoService.contarIngresosHoy());
        dto.setAsistenciasHoy(asistenciaService.contarAsistenciasHoy());
        dto.setPersonasRestringidas(personaService.contarRestringidos());
        dto.setTotalPersonas(personaService.contarActivos());
        return dto;
    }
}
