package com.umg.biometrico.controller;

import com.umg.biometrico.model.Curso;
import com.umg.biometrico.service.CursoService;
import com.umg.biometrico.service.PersonaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cursos")
@RequiredArgsConstructor
public class CursoController {

    private final CursoService cursoService;
    private final PersonaService personaService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("cursos", cursoService.listarActivos());
        model.addAttribute("activeMenu", "cursos");
        return "cursos/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("curso", new Curso());
        model.addAttribute("catedraticos", personaService.listarCatedraticos());
        model.addAttribute("activeMenu", "cursos");
        return "cursos/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Curso curso,
                          @RequestParam Long catedraticoId,
                          RedirectAttributes redirectAttributes) {
        personaService.buscarPorId(catedraticoId).ifPresent(curso::setCatedratico);
        cursoService.guardar(curso);
        redirectAttributes.addFlashAttribute("success", "Curso guardado correctamente.");
        return "redirect:/cursos";
    }

    @GetMapping("/{id}")
    public String ver(@PathVariable Long id, Model model) {
        cursoService.buscarPorId(id).ifPresent(c -> {
            model.addAttribute("curso", c);
            model.addAttribute("estudiantes", cursoService.listarEstudiantesDeCurso(id));
            model.addAttribute("todosEstudiantes", personaService.listarEstudiantes());
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
}
