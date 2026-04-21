package com.umg.biometrico.controller;

import com.umg.biometrico.dto.DashboardDTO;
import com.umg.biometrico.repository.CursoRepository;
import com.umg.biometrico.service.AsistenciaService;
import com.umg.biometrico.service.DashboardService;
import com.umg.biometrico.service.PersonaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final PersonaService personaService;
    private final CursoRepository cursoRepository;
    private final AsistenciaService asistenciaService;

    @GetMapping
    public String dashboard(Model model, Principal principal) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isEstudiante = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ESTUDIANTE".equals(a.getAuthority()));

        model.addAttribute("fechaActual",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        model.addAttribute("activeMenu", "dashboard");

        if (isEstudiante && principal != null) {
            return personaService.buscarPorCorreo(principal.getName()).map(estudiante -> {
                var cursos = cursoRepository.findCursosByEstudiante(estudiante.getId());
                Map<com.umg.biometrico.model.Curso,
                    java.util.List<com.umg.biometrico.model.Asistencia>> historial = new LinkedHashMap<>();
                for (var c : cursos) {
                    historial.put(c, asistenciaService.obtenerHistorialEstudiante(estudiante.getId(), c.getId()));
                }
                model.addAttribute("cursos", cursos);
                model.addAttribute("historial", historial);
                model.addAttribute("estudiante", estudiante);
                return "dashboard/estudiante";
            }).orElse("redirect:/auth/login");
        }

        // Admin / Catedrático — dashboard normal
        DashboardDTO stats = dashboardService.obtenerEstadisticas();
        model.addAttribute("stats", stats);
        model.addAttribute("ultimosEstudiantes", personaService.listarUltimas5());
        return "dashboard/index";
    }
}
