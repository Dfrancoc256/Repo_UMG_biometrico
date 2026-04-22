package com.umg.biometrico.controller;

import com.umg.biometrico.model.Curso;
import com.umg.biometrico.model.Persona;
import com.umg.biometrico.repository.PersonaRepository;
import com.umg.biometrico.service.AsistenciaService;
import com.umg.biometrico.service.CursoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class EstudianteController {

    private final PersonaRepository personaRepository;
    private final CursoService cursoService;
    private final AsistenciaService asistenciaService;

    @GetMapping("/mis-cursos")
    public String misCursos(Authentication authentication, Model model) {
        String correo = authentication.getName();

        Persona estudiante = personaRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));

        List<Curso> cursos = cursoService.obtenerCursosDeEstudiante(estudiante.getId());

        model.addAttribute("cursos", cursos);
        model.addAttribute("pageTitle", "Mis Cursos");
        model.addAttribute("activeMenu", "mis-cursos");

        return "cursos/mis-cursos";
    }

    @GetMapping("/mi-asistencia")
    public String miAsistencia(Authentication authentication, Model model) {
        String correo = authentication.getName();

        Persona estudiante = personaRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));

        model.addAttribute("asistencias", asistenciaService.obtenerAsistenciaDeEstudiante(estudiante.getId()));
        model.addAttribute("pageTitle", "Mi Asistencia");
        model.addAttribute("activeMenu", "mi-asistencia");

        return "asistencia/mi-asistencia";
    }
    
}