package com.umg.biometrico.controller;

import com.umg.biometrico.dto.AsistenciaDTO;
import com.umg.biometrico.model.Asistencia;
import com.umg.biometrico.model.Curso;
import com.umg.biometrico.model.CursoSeccionAsignacion;
import com.umg.biometrico.repository.CursoRepository;
import com.umg.biometrico.repository.CursoSeccionAsignacionRepository;
import com.umg.biometrico.service.AsistenciaService;
import com.umg.biometrico.service.CursoService;
import com.umg.biometrico.service.EmailService;
import com.umg.biometrico.service.PersonaService;
import com.umg.biometrico.service.PdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.umg.biometrico.model.Persona;

import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/asistencia")
@RequiredArgsConstructor
public class AsistenciaController {

    private final AsistenciaService asistenciaService;
    private final CursoService cursoService;
    private final PdfService pdfService;
    private final EmailService emailService;
    private final PersonaService personaService;
    private final CursoRepository cursoRepository;
    private final CursoSeccionAsignacionRepository asignacionRepo;

    @GetMapping
    public String listarCursos(Model model, Principal principal) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isEstudiante = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ESTUDIANTE".equals(a.getAuthority()));

        if (isEstudiante && principal != null) {
            return personaService.buscarPorCorreo(principal.getName()).map(estudiante -> {
                List<Curso> cursos = cursoRepository.findCursosByEstudiante(estudiante.getId());
                Map<Curso, List<Asistencia>> historial = new LinkedHashMap<>();
                for (Curso c : cursos) {
                    List<Asistencia> registros = asistenciaService
                            .obtenerHistorialEstudiante(estudiante.getId(), c.getId());
                    historial.put(c, registros);
                }
                model.addAttribute("historial", historial);
                model.addAttribute("activeMenu", "asistencia");
                return "asistencia/mi-asistencia";
            }).orElse("redirect:/dashboard");
        }

        model.addAttribute("cursos", cursoService.listarActivos());
        model.addAttribute("activeMenu", "asistencia");
        return "asistencia/cursos";
    }

    @GetMapping("/curso/{cursoId}")
    public String arbolAsistencia(@PathVariable Long cursoId,
                                  @RequestParam(required = false)
                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                  Model model,
                                  Principal principal,
                                  RedirectAttributes redirectAttributes) {

        Optional<Curso> cursoOpt = cursoService.buscarPorId(cursoId);
        if (cursoOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Curso no encontrado.");
            return "redirect:/asistencia";
        }

        Curso curso = cursoOpt.get();
        LocalDate fechaEfectiva = fecha != null ? fecha : LocalDate.now();
        List<AsistenciaDTO> arbol = asistenciaService.obtenerArbolAsistencia(cursoId, fechaEfectiva);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isCatedratico = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_CATEDRATICO".equals(a.getAuthority()));

        Persona catedraticoRaiz = null;
        final List<AsistenciaDTO> arbolVisible = new ArrayList<>();
        final List<String> seccionesAsignadas = new ArrayList<>();

        if (isCatedratico && principal != null) {
            Optional<Persona> catOpt = personaService.buscarPorCorreo(principal.getName());
            if (catOpt.isPresent()) {
                Persona catActual = catOpt.get();
                catedraticoRaiz = catActual;

                try {
                    List<CursoSeccionAsignacion> misAsignaciones = asignacionRepo
                            .findByCurso_Id(cursoId).stream()
                            .filter(a -> a.getCatedratico().getId().equals(catActual.getId()))
                            .toList();

                    if (!misAsignaciones.isEmpty()) {
                        seccionesAsignadas.clear();
                        seccionesAsignadas.addAll(misAsignaciones.stream()
                                .map(CursoSeccionAsignacion::getSeccion).toList());
                        arbolVisible.clear();
                        arbolVisible.addAll(arbol.stream()
                                .filter(n -> n.getSeccion() != null
                                        && seccionesAsignadas.contains(n.getSeccion()))
                                .toList());
                    } else {
                        arbolVisible.addAll(arbol);
                    }
                } catch (Exception ignored) {
                    arbolVisible.addAll(arbol);
                }
            }
        } else {
            arbolVisible.addAll(arbol);
            try {
                List<CursoSeccionAsignacion> asignaciones = asignacionRepo.findByCurso_Id(cursoId);
                if (!asignaciones.isEmpty()) {
                    catedraticoRaiz = asignaciones.get(0).getCatedratico();
                }
            } catch (Exception ignored) {}

            if (catedraticoRaiz == null) {
                catedraticoRaiz = curso.getCatedratico();
            }
        }

        model.addAttribute("arbol", arbolVisible);
        model.addAttribute("seccionesAsignadas", seccionesAsignadas);
        if (catedraticoRaiz != null) {
            model.addAttribute("catedraticoActual", catedraticoRaiz);
        }
        model.addAttribute("curso", curso);
        model.addAttribute("fecha", fechaEfectiva);
        model.addAttribute("fechaStr", fechaEfectiva.toString());
        model.addAttribute("activeMenu", "asistencia");
        if (principal != null) {
            model.addAttribute("usuarioCorreo", principal.getName());
        }
        return "asistencia/arbol";
    }

    @PostMapping("/curso/{cursoId}/confirmar")
    public String confirmar(@PathVariable Long cursoId,
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                            @RequestParam(required = false) List<Long> presentesIds,
                            RedirectAttributes redirectAttributes) {
        asistenciaService.confirmarAsistencia(cursoId, fecha, presentesIds);
        redirectAttributes.addFlashAttribute("success", "Asistencia confirmada para el " + fecha);
        return "redirect:/asistencia/curso/" + cursoId + "?fecha=" + fecha;
    }

    @PostMapping("/curso/{cursoId}/enviar-asistencia")
    public String enviarAsistencia(@PathVariable Long cursoId,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                   @RequestParam String correoDestino,
                                   RedirectAttributes redirectAttributes) {
        return cursoService.buscarPorId(cursoId).map(curso -> {
            try {
                List<Asistencia> lista = asistenciaService.obtenerAsistenciasPorCursoYFecha(cursoId, fecha);
                byte[] pdf = pdfService.generarReporteAsistenciaPdf(curso, fecha, lista);
                emailService.enviarReporteAsistencia(correoDestino, curso.getNombre(), fecha.toString(), pdf);
                redirectAttributes.addFlashAttribute("success",
                        "Reporte de asistencia enviado exitosamente a: " + correoDestino);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error",
                        "Error al enviar el reporte: " + e.getMessage());
            }
            return "redirect:/asistencia/curso/" + cursoId + "?fecha=" + fecha;
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("error", "Curso no encontrado.");
            return "redirect:/asistencia";
        });
    }

    @GetMapping("/curso/{cursoId}/reporte-pdf")
    public ResponseEntity<byte[]> reportePdf(@PathVariable Long cursoId,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return cursoService.buscarPorId(cursoId).map(curso -> {
            try {
                List<Asistencia> lista = asistenciaService.obtenerAsistenciasPorCursoYFecha(cursoId, fecha);
                byte[] pdf = pdfService.generarReporteAsistenciaPdf(curso, fecha, lista);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"asistencia_" + cursoId + "_" + fecha + ".pdf\"")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(pdf);
            } catch (Exception e) {
                return ResponseEntity.status(500).body(new byte[0]);
            }
        }).orElse(ResponseEntity.status(404).body(new byte[0]));
    }
}