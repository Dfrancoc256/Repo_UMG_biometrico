package com.umg.biometrico.controller;

import com.umg.biometrico.model.Instalacion;
import com.umg.biometrico.model.Puerta;
import com.umg.biometrico.model.RegistroIngreso;
import com.umg.biometrico.repository.InstalacionRepository;
import com.umg.biometrico.service.RegistroIngresoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final RegistroIngresoService registroIngresoService;
    private final InstalacionRepository instalacionRepository;

    @GetMapping
    public String inicio(Model model) {
        model.addAttribute("instalaciones", instalacionRepository.findAll());
        model.addAttribute("activeMenu", "reportes");
        return "reportes/index";
    }

    @GetMapping("/puerta")
    public String reportePuerta(@RequestParam(required = false) Long instalacionId,
                                @RequestParam(required = false) Long puertaId,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                @RequestParam(required = false, defaultValue = "desc") String orden,
                                Model model) {
        model.addAttribute("instalaciones", instalacionRepository.findAll());
        model.addAttribute("activeMenu", "reportes");

        if (instalacionId != null) {
            instalacionRepository.findById(instalacionId).ifPresent(inst -> {
                model.addAttribute("instalacion", inst);
                model.addAttribute("puertas", inst.getPuertas());
            });
        }

        if (puertaId != null && fecha != null) {
            List<RegistroIngreso> registros = registroIngresoService
                    .obtenerIngresosPorPuertaYFecha(puertaId, fecha, orden);
            model.addAttribute("registros", registros);
            model.addAttribute("puertaId", puertaId);
            model.addAttribute("fecha", fecha);
        }

        if (puertaId != null) {
            model.addAttribute("fechasConIngreso",
                    registroIngresoService.obtenerFechasConIngreso(puertaId));
        }

        model.addAttribute("orden", orden);
        model.addAttribute("instalacionId", instalacionId);
        model.addAttribute("puertaId", puertaId);
        return "reportes/puerta";
    }

    @GetMapping("/salon")
    public String reporteSalon(@RequestParam(required = false) Long instalacionId,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                               Model model) {
        model.addAttribute("instalaciones", instalacionRepository.findAll());
        model.addAttribute("activeMenu", "reportes");

        if (instalacionId != null) {
            instalacionRepository.findById(instalacionId).ifPresent(inst -> {
                model.addAttribute("instalacion", inst);
                model.addAttribute("puertas", inst.getPuertas());
                List<RegistroIngreso> registros;
                if (fecha != null) {
                    registros = registroIngresoService.obtenerIngresosPorInstalacionYFecha(instalacionId, fecha);
                } else {
                    registros = registroIngresoService.obtenerIngresosASalones(instalacionId);
                }
                model.addAttribute("registros", registros);
            });
            model.addAttribute("instalacionId", instalacionId);
        }
        model.addAttribute("fecha", fecha);
        return "reportes/salon";
    }

    @GetMapping("/historico")
    public String historicoArbol(Model model) {
        model.addAttribute("instalaciones", instalacionRepository.findAll());
        model.addAttribute("activeMenu", "reportes");
        return "reportes/historico";
    }
}
