package com.umg.biometrico.controller;

import com.umg.biometrico.model.Curso;
import com.umg.biometrico.model.Persona;
import com.umg.biometrico.repository.CursoEstudianteRepository;
import com.umg.biometrico.repository.PersonaRepository;
import com.umg.biometrico.service.CursoService;
import com.umg.biometrico.service.PersonaService;
import com.umg.biometrico.repository.PersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/enrolamiento")
@RequiredArgsConstructor
public class EnrolamientoController {

    private final PersonaService personaService;
    private final CursoService cursoService;
    private final CursoEstudianteRepository cursoEstudianteRepository;
    private final PersonaRepository personaRepository;

    private static final int MAX_CURSOS = 5;

    @GetMapping
    public String mostrarFormulario() {
        return "enrolamiento/buscar";
    }

    @PostMapping("/buscar")
    public String buscarPorCarnet(@RequestParam String carnet, Model model) {

        carnet = carnet != null ? carnet.trim() : "";

        if (carnet.isBlank()) {
            model.addAttribute("errorMsg", "Debes ingresar tu número de carnet.");
            return "enrolamiento/buscar";
        }

        if (!carnet.toUpperCase().startsWith("UMG-")) {
            carnet = "UMG-" + carnet;
        }

        Optional<Persona> opt = personaRepository
                .findByNumeroCarnetAndRol_NombreIgnoreCase(carnet, "ESTUDIANTE");

        if (opt.isEmpty()) {
            model.addAttribute("errorMsg", "No se encontró ningún estudiante con el carnet: " + carnet);
            model.addAttribute("carnetBuscado", carnet);
            return "enrolamiento/buscar";
        }

        Persona estudiante = opt.get();

        if (estudiante.getRol() == null || !"ESTUDIANTE".equalsIgnoreCase(estudiante.getRol().getNombre())) {
            model.addAttribute("errorMsg", "El carnet ingresado no corresponde a un estudiante activo.");
            model.addAttribute("carnetBuscado", carnet);
            return "enrolamiento/buscar";
        }

        if (!Boolean.TRUE.equals(estudiante.getActivo())) {
            model.addAttribute("errorMsg", "La cuenta asociada a este carnet está inactiva.");
            model.addAttribute("carnetBuscado", carnet);
            return "enrolamiento/buscar";
        }

        List<Long> idsInscritos = cursoEstudianteRepository.findByEstudiante_Id(estudiante.getId())
                .stream().map(ce -> ce.getCurso().getId()).toList();

        List<Curso> todosLosCursos = cursoService.listarActivos();
        int slotsDisponibles = Math.max(0, MAX_CURSOS - idsInscritos.size());

        model.addAttribute("estudiante", estudiante);
        model.addAttribute("cursos", todosLosCursos);
        model.addAttribute("cursosInscritos", idsInscritos);
        model.addAttribute("slotsDisponibles", slotsDisponibles);
        model.addAttribute("maxCursos", MAX_CURSOS);
        return "enrolamiento/seleccionar";
    }

    @PostMapping("/guardar")
    public String guardarEnrolamiento(@RequestParam Long estudianteId,
                                      @RequestParam(required = false) List<Long> cursosSeleccionados,
                                      RedirectAttributes redirectAttributes) {
        if (cursosSeleccionados == null || cursosSeleccionados.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMsg", "Debes seleccionar al menos un curso nuevo.");
            return "redirect:/enrolamiento";
        }

        long yaInscritos = cursoEstudianteRepository.findByEstudiante_Id(estudianteId).size();

        if (yaInscritos + cursosSeleccionados.size() > MAX_CURSOS) {
            redirectAttributes.addFlashAttribute("errorMsg",
                    "Límite excedido: solo puedes inscribirte en " + MAX_CURSOS + " cursos en total. " +
                    "Ya tienes " + yaInscritos + " inscrito(s). Puedes agregar máximo " +
                    (MAX_CURSOS - yaInscritos) + " más.");
            return "redirect:/enrolamiento";
        }

        for (Long cursoId : cursosSeleccionados) {
            cursoService.inscribirEstudiante(cursoId, estudianteId);
        }

        redirectAttributes.addFlashAttribute("successMsg",
                "¡Enrolamiento exitoso! Te inscribiste en " + cursosSeleccionados.size() +
                " curso(s). Ingresa al sistema con tus credenciales para verlos.");
        return "redirect:/enrolamiento";
    }
}
