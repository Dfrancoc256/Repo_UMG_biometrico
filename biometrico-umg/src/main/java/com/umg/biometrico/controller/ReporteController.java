package com.umg.biometrico.controller;

import com.umg.biometrico.model.Instalacion;
import com.umg.biometrico.model.Persona;
import com.umg.biometrico.model.Puerta;
import com.umg.biometrico.model.RegistroIngreso;
import com.umg.biometrico.repository.InstalacionRepository;
import com.umg.biometrico.service.EmailService;
import com.umg.biometrico.service.PdfService;
import com.umg.biometrico.service.RegistroIngresoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final RegistroIngresoService registroIngresoService;
    private final InstalacionRepository instalacionRepository;
    private final PdfService pdfService;
    private final EmailService emailService;

    private static final Set<String> ORDENES_VALIDOS = Set.of("asc", "desc");

    private String validarOrden(String orden) {
        if (orden == null || !ORDENES_VALIDOS.contains(orden.toLowerCase())) return "desc";
        return orden.toLowerCase();
    }

    // ── Dashboard de reportes ────────────────────────────────────────────────
    @GetMapping
    public String inicio(Model model) {
        model.addAttribute("activeMenu", "reportes");
        return "reportes/index";
    }

    // ── REPORTE 1: Histórico por puerta — árbol fechas ───────────────────────
    @GetMapping("/historico-puerta")
    public String historicoPuerta(
            @RequestParam(required = false) Long instalacionId,
            @RequestParam(required = false) Long puertaId,
            Model model) {

        model.addAttribute("activeMenu", "rep-hist-puerta");
        model.addAttribute("instalaciones", instalacionRepository.findAll());
        model.addAttribute("instalacionId", instalacionId);
        model.addAttribute("puertaId", puertaId);

        if (instalacionId != null) {
            instalacionRepository.findByIdWithPuertas(instalacionId).ifPresent(inst -> {
                model.addAttribute("instalacion", inst);
                model.addAttribute("puertas", inst.getPuertas());
            });
        }

        if (puertaId != null) {
            List<RegistroIngreso> todos = registroIngresoService.obtenerTodosIngresosPorPuerta(puertaId);

            Function<RegistroIngreso, LocalDate> classifier = r -> r.getFechaHora().toLocalDate();
            Supplier<TreeMap<LocalDate, List<RegistroIngreso>>> mapFactory =
                    () -> new TreeMap<>(Comparator.<LocalDate>reverseOrder());

            TreeMap<LocalDate, List<RegistroIngreso>> porFecha = todos.stream()
                    .filter(r -> r.getFechaHora() != null)
                    .collect(Collectors.groupingBy(classifier, mapFactory, Collectors.toList()));

            model.addAttribute("registrosPorFecha", porFecha);
        }

        return "reportes/historico-puerta";
    }

    @PostMapping("/historico-puerta/email")
    public String emailHistoricoPuerta(
            Authentication auth,
            @RequestParam Long puertaId,
            @RequestParam(required = false) Long instalacionId,
            @RequestParam(required = false) String puertaNombre,
            @RequestParam(required = false) String instalacionNombre,
            RedirectAttributes redirectAttrs) {

        String correo = auth.getName();
        try {
            List<RegistroIngreso> registros = registroIngresoService.obtenerTodosIngresosPorPuerta(puertaId);
            String sub = safe(instalacionNombre) + " · " + safe(puertaNombre);
            byte[] pdf = pdfService.generarReporteIngreso("Histórico de Ingresos por Puerta", sub, registros);
            emailService.enviarReporteIngreso(correo, "Histórico de Ingresos por Puerta", sub, pdf);
            redirectAttrs.addFlashAttribute("success", "Reporte enviado a " + correo);
        } catch (Exception e) {
            log.error("Error enviando reporte histórico puerta", e);
            redirectAttrs.addFlashAttribute("error", "Error al enviar el reporte. Verifique la configuración de correo.");
        }
        return buildRedirect("/reportes/historico-puerta", "instalacionId", instalacionId, "puertaId", puertaId);
    }

    // ── REPORTE 2: Histórico por salón — árbol nivel → salón → personas ──────
    @GetMapping("/historico-salon")
    public String historicoSalon(
            @RequestParam(required = false) Long instalacionId,
            Model model) {

        model.addAttribute("activeMenu", "rep-hist-salon");
        model.addAttribute("instalaciones", instalacionRepository.findAll());
        model.addAttribute("instalacionId", instalacionId);

        if (instalacionId != null) {
            instalacionRepository.findByIdWithPuertas(instalacionId).ifPresent(inst -> {
                model.addAttribute("instalacion", inst);

                List<Puerta> salones = inst.getPuertas().stream()
                        .filter(p -> Boolean.TRUE.equals(p.getEsSalon()))
                        .collect(Collectors.toList());

                LinkedHashMap<String, List<Map<String, Object>>> arbolSalones = new LinkedHashMap<>();
                for (Puerta salon : salones) {
                    String nv = (salon.getNivel() != null && !salon.getNivel().isBlank())
                            ? salon.getNivel() : "Sin Nivel";
                    List<Persona> personas = registroIngresoService.obtenerPersonasEnSalon(salon.getId());
                    Map<String, Object> salonData = new HashMap<>();
                    salonData.put("salon", salon);
                    salonData.put("personas", personas);
                    arbolSalones.computeIfAbsent(nv, k -> new ArrayList<>()).add(salonData);
                }
                model.addAttribute("arbolSalones", arbolSalones);
            });
        }

        return "reportes/historico-salon";
    }

    @PostMapping("/historico-salon/email")
    public String emailHistoricoSalon(
            Authentication auth,
            @RequestParam Long instalacionId,
            @RequestParam(required = false) String instalacionNombre,
            RedirectAttributes redirectAttrs) {

        String correo = auth.getName();
        try {
            List<RegistroIngreso> registros = registroIngresoService.obtenerIngresosASalones(instalacionId);
            String sub = safe(instalacionNombre) + " — Todos los salones";
            byte[] pdf = pdfService.generarReporteIngreso("Histórico de Ingresos por Salón", sub, registros);
            emailService.enviarReporteIngreso(correo, "Histórico de Ingresos por Salón", sub, pdf);
            redirectAttrs.addFlashAttribute("success", "Reporte enviado a " + correo);
        } catch (Exception e) {
            log.error("Error enviando reporte histórico salón", e);
            redirectAttrs.addFlashAttribute("error", "Error al enviar el reporte. Verifique la configuración de correo.");
        }
        return "redirect:/reportes/historico-salon?instalacionId=" + instalacionId;
    }

    // ── Rutas antiguas: redirigen a las vistas históricas ────────────────────
    @GetMapping("/puerta")
    public String redirigirPuerta(@RequestParam(required = false) Long instalacionId,
                                  @RequestParam(required = false) Long puertaId) {
        StringBuilder url = new StringBuilder("redirect:/reportes/historico-puerta");
        String sep = "?";
        if (instalacionId != null) { url.append(sep).append("instalacionId=").append(instalacionId); sep = "&"; }
        if (puertaId != null)      { url.append(sep).append("puertaId=").append(puertaId); }
        return url.toString();
    }

    @GetMapping("/salon")
    public String redirigirSalon(@RequestParam(required = false) Long instalacionId) {
        if (instalacionId != null)
            return "redirect:/reportes/historico-salon?instalacionId=" + instalacionId;
        return "redirect:/reportes/historico-salon";
    }

    // ── Legacy para compatibilidad ────────────────────────────────────────────
    @GetMapping("/historico")
    public String historicoArbol(Model model) {
        model.addAttribute("instalaciones", instalacionRepository.findAllWithPuertas());
        model.addAttribute("activeMenu", "rep-hist-puerta");
        return "reportes/historico";
    }

    private String safe(String s) { return s != null ? s : ""; }

    private String buildRedirect(String base, Object... kvPairs) {
        StringBuilder sb = new StringBuilder("redirect:").append(base).append("?");
        for (int i = 0; i < kvPairs.length - 1; i += 2) {
            if (kvPairs[i + 1] != null)
                sb.append(kvPairs[i]).append("=").append(kvPairs[i + 1]).append("&");
        }
        return sb.toString().replaceAll("&$", "");
    }
}
