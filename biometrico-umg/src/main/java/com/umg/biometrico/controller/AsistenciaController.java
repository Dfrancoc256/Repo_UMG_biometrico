package com.umg.biometrico.controller;

import com.umg.biometrico.dto.AsistenciaDTO;
import com.umg.biometrico.model.Asistencia;
import com.umg.biometrico.service.AsistenciaService;
import com.umg.biometrico.service.CursoService;
import com.umg.biometrico.service.PdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/asistencia")
@RequiredArgsConstructor
public class AsistenciaController {

    private final AsistenciaService asistenciaService;
    private final CursoService cursoService;
    private final PdfService pdfService;

    @GetMapping
    public String listarCursos(Model model) {
        model.addAttribute("cursos", cursoService.listarActivos());
        model.addAttribute("activeMenu", "asistencia");
        return "asistencia/cursos";
    }

    @GetMapping("/curso/{cursoId}")
    public String arbolAsistencia(@PathVariable Long cursoId,
                                  @RequestParam(required = false)
                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        return cursoService.buscarPorId(cursoId).map(curso -> {
            if (fecha == null) {
                LocalDate hoy = LocalDate.now();
                List<AsistenciaDTO> arbol = asistenciaService.obtenerArbolAsistencia(cursoId, hoy);
                model.addAttribute("curso", curso);
                model.addAttribute("arbol", arbol);
                model.addAttribute("fecha", hoy);
                model.addAttribute("fechaStr", hoy.toString());
            } else {
                List<AsistenciaDTO> arbol = asistenciaService.obtenerArbolAsistencia(cursoId, fecha);
                model.addAttribute("curso", curso);
                model.addAttribute("arbol", arbol);
                model.addAttribute("fecha", fecha);
                model.addAttribute("fechaStr", fecha.toString());
            }
            model.addAttribute("activeMenu", "asistencia");
            return "asistencia/arbol";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("error", "Curso no encontrado.");
            return "redirect:/asistencia";
        });
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