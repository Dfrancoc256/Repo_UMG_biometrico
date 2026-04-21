package com.umg.biometrico.controller;

import com.umg.biometrico.model.Curso;
import com.umg.biometrico.model.CursoSeccionAsignacion;
import com.umg.biometrico.repository.CursoSeccionAsignacionRepository;
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
    private final CursoSeccionAsignacionRepository asignacionRepo;

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

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        cursoService.buscarPorId(id).ifPresent(c -> model.addAttribute("curso", c));
        model.addAttribute("catedraticos", personaService.listarCatedraticos());
        model.addAttribute("activeMenu", "cursos");
        return "cursos/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Curso curso,
                          @RequestParam(required = false) Long catedraticoId,
                          RedirectAttributes redirectAttributes) {
        if (catedraticoId != null) {
            personaService.buscarPorId(catedraticoId).ifPresent(curso::setCatedratico);
        }
        Curso guardado = cursoService.guardar(curso);
        redirectAttributes.addFlashAttribute("success", "Curso guardado correctamente.");
        return "redirect:/cursos/" + guardado.getId();
    }

    @GetMapping("/{id}")
    public String ver(@PathVariable Long id, Model model) {
        cursoService.buscarPorId(id).ifPresent(c -> {
            model.addAttribute("curso", c);
            model.addAttribute("estudiantes", cursoService.listarEstudiantesDeCurso(id));
            model.addAttribute("todosEstudiantes", personaService.listarEstudiantes());
            model.addAttribute("catedraticos", personaService.listarCatedraticos());
            // Tabla curso_seccion_asignacion puede no existir aún (pendiente de migración SQL)
            try {
                model.addAttribute("asignaciones", asignacionRepo.findByCurso_Id(id));
            } catch (Exception e) {
                model.addAttribute("asignaciones", java.util.List.of());
            }
        });
        model.addAttribute("activeMenu", "cursos");
        return "cursos/detalle";
    }

    @PostMapping("/{id}/asignar-seccion")
    public String asignarSeccion(@PathVariable Long id,
                                 @RequestParam String seccion,
                                 @RequestParam Long catedraticoId,
                                 RedirectAttributes ra) {
        cursoService.buscarPorId(id).ifPresent(curso -> {
            personaService.buscarPorId(catedraticoId).ifPresent(cat -> {
                CursoSeccionAsignacion a = asignacionRepo
                        .findByCurso_IdAndSeccion(id, seccion)
                        .orElseGet(CursoSeccionAsignacion::new);
                a.setCurso(curso);
                a.setSeccion(seccion);
                a.setCatedratico(cat);
                asignacionRepo.save(a);
            });
        });
        ra.addFlashAttribute("success", "Sección " + seccion + " asignada.");
        return "redirect:/cursos/" + id;
    }

    @PostMapping("/{id}/quitar-seccion")
    public String quitarSeccion(@PathVariable Long id,
                                @RequestParam String seccion,
                                RedirectAttributes ra) {
        asignacionRepo.findByCurso_IdAndSeccion(id, seccion)
                .ifPresent(asignacionRepo::delete);
        ra.addFlashAttribute("success", "Asignación de sección " + seccion + " eliminada.");
        return "redirect:/cursos/" + id;
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
