package com.umg.biometrico.controller;


import com.umg.biometrico.repository.CamaraRepository;
import com.umg.biometrico.model.Curso;
import com.umg.biometrico.model.Persona;
import com.umg.biometrico.service.CursoService;
import com.umg.biometrico.service.PersonaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/cursos")
@RequiredArgsConstructor
public class CursoController {

    private final CursoService cursoService;
    private final PersonaService personaService;
    private final CamaraRepository camaraRepository;


    @GetMapping
    public String listar(Model model, Authentication authentication) {

        String correo = authentication.getName();
        Persona persona = personaService.buscarPorCorreo(correo).orElse(null);

        boolean esAdmin = persona != null && persona.getRol() != null
                && persona.getRol().getNombre().equalsIgnoreCase("ADMIN");

        if (persona != null && persona.getRol() != null
                && persona.getRol().getNombre().equalsIgnoreCase("CATEDRATICO")) {
            model.addAttribute("cursos", cursoService.listarPorCatedratico(persona.getId()));
        } else {
            model.addAttribute("cursos", cursoService.listarActivos());
        }

        if (esAdmin) {
            model.addAttribute("cursosPorCarrera", cursoService.listarActivosAgrupadosPorCarrera());
        }

        model.addAttribute("activeMenu", "cursos");
        return "cursos/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("curso", new Curso());
        List<Persona> catedraticos = personaService.listarCatedraticos();
        model.addAttribute("catedraticos", catedraticos);
        model.addAttribute("cursosPerCatedratico", buildCursosPerCatedratico(catedraticos));
        model.addAttribute("codigoPreview", cursoService.generarCodigoPreview());
        model.addAttribute("activeMenu", "cursos");
        return "cursos/formulario";
    }

    @GetMapping("/preview-codigo")
    @ResponseBody
    public String previewCodigo() {
        return cursoService.generarCodigoPreview();
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        cursoService.buscarPorId(id).ifPresent(c -> model.addAttribute("curso", c));
        List<Persona> catedraticos = personaService.listarCatedraticos();
        model.addAttribute("catedraticos", catedraticos);
        model.addAttribute("cursosPerCatedratico", buildCursosPerCatedratico(catedraticos));
        model.addAttribute("activeMenu", "cursos");
        return "cursos/formulario";
    }

    private Map<Long, Long> buildCursosPerCatedratico(List<Persona> catedraticos) {
        return catedraticos.stream().collect(
                Collectors.toMap(Persona::getId, c -> cursoService.contarCursosPorCatedratico(c.getId())));
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Curso curso,
                          @RequestParam(required = false) Long catedraticoId,
                          RedirectAttributes redirectAttributes) {
        if (catedraticoId != null) {
            personaService.buscarPorId(catedraticoId).ifPresent(curso::setCatedratico);
        }
        try {
            Curso guardado = cursoService.guardar(curso);
            redirectAttributes.addFlashAttribute("success", "Curso guardado correctamente.");
            return "redirect:/cursos/" + guardado.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/cursos/nuevo";
        }
    }

@GetMapping("/{id}")
    public String ver(@PathVariable Long id, Model model) {
        cursoService.buscarPorId(id).ifPresent(c -> {
            model.addAttribute("curso", c);
            model.addAttribute("estudiantes", cursoService.listarEstudiantesDeCurso(id));
            model.addAttribute("todosEstudiantes", personaService.listarEstudiantes());

            // Catedráticos disponibles para asignar
            model.addAttribute("catedraticos", personaService.listarCatedraticos());

            // Cámaras disponibles para habilitar asistencia biométrica
            model.addAttribute("camaras", camaraRepository.findByActivaTrue());
        });

        model.addAttribute("activeMenu", "cursos");
        return "cursos/detalle";
    }

    @PostMapping("/{id}/inscribir")
    public String inscribir(@PathVariable Long id,
                            @RequestParam Long estudianteId,
                            RedirectAttributes redirectAttributes) {
        cursoService.inscribirEstudiante(id, estudianteId);
        redirectAttributes.addFlashAttribute("success", "Estudiante inscrito.");
        return "redirect:/cursos/" + id;
    }

    @PostMapping("/{id}/desinscribir")
    public String desinscribir(@PathVariable Long id,
                               @RequestParam Long estudianteId,
                               RedirectAttributes redirectAttributes) {
        cursoService.desinscribirEstudiante(id, estudianteId);
        redirectAttributes.addFlashAttribute("success", "Estudiante removido del curso.");
        return "redirect:/cursos/" + id;
    }

    @PostMapping("/{id}/asignar-seccion")
    public String asignarCatedraticoPorSeccion(@PathVariable Long id,
                                               @RequestParam String seccion,
                                               @RequestParam Long catedraticoId,
                                               RedirectAttributes redirectAttributes) {

        cursoService.buscarPorId(id).ifPresent(curso -> {

            personaService.buscarPorId(catedraticoId).ifPresent(catedratico -> {

                curso.setCatedratico(catedratico);

                curso.setSeccion(seccion);

                cursoService.guardar(curso);
            });
        });

        redirectAttributes.addFlashAttribute("success",
                "Catedrático asignado correctamente.");

        return "redirect:/cursos/" + id;
    }
}