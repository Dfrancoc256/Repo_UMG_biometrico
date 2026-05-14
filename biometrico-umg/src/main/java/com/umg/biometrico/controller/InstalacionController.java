package com.umg.biometrico.controller;

import com.umg.biometrico.model.Instalacion;
import com.umg.biometrico.model.Puerta;
import com.umg.biometrico.repository.InstalacionRepository;
import com.umg.biometrico.repository.PuertaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/instalaciones")
@RequiredArgsConstructor
public class InstalacionController {

    private final InstalacionRepository instalacionRepository;
    private final PuertaRepository puertaRepository;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("instalaciones", instalacionRepository.findAll());
        model.addAttribute("activeMenu", "instalaciones");
        return "instalaciones/lista";
    }

    @GetMapping("/nueva")
    public String nueva(Model model) {
        model.addAttribute("instalacion", new Instalacion());
        model.addAttribute("activeMenu", "instalaciones");
        return "instalaciones/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Instalacion instalacion, RedirectAttributes ra) {
        instalacionRepository.save(instalacion);
        ra.addFlashAttribute("success", "Instalación guardada.");
        return "redirect:/instalaciones";
    }

    @GetMapping("/{id}")
    public String ver(@PathVariable Long id, Model model) {
        instalacionRepository.findById(id).ifPresent(inst -> {
            model.addAttribute("instalacion", inst);
            model.addAttribute("puertas", puertaRepository.findByInstalacionId(id));
        });
        model.addAttribute("activeMenu", "instalaciones");
        return "instalaciones/detalle";
    }

    @GetMapping("/{id}/puerta/nueva")
    public String nuevaPuerta(@PathVariable Long id, Model model) {
        instalacionRepository.findById(id).ifPresent(inst -> model.addAttribute("instalacion", inst));
        model.addAttribute("puerta", new Puerta());
        model.addAttribute("activeMenu", "instalaciones");
        return "instalaciones/puerta-formulario";
    }

    @PostMapping("/{id}/puerta/guardar")
    public String guardarPuerta(@PathVariable Long id,
                                 @ModelAttribute Puerta puerta,
                                 RedirectAttributes ra) {
        instalacionRepository.findById(id).ifPresent(inst -> {
            puerta.setInstalacion(inst);
            puertaRepository.save(puerta);
        });
        ra.addFlashAttribute("success", "Acceso/Salón registrado.");
        return "redirect:/instalaciones/" + id;
    }

    @GetMapping("/{id}/puerta/{puertaId}/editar")
    public String editarPuerta(@PathVariable Long id, @PathVariable Long puertaId, Model model) {
        instalacionRepository.findById(id).ifPresent(inst -> model.addAttribute("instalacion", inst));
        puertaRepository.findById(puertaId).ifPresent(p -> model.addAttribute("puerta", p));
        model.addAttribute("activeMenu", "instalaciones");
        return "instalaciones/puerta-formulario";
    }

    @PostMapping("/{id}/puerta/{puertaId}/guardar")
    public String actualizarPuerta(@PathVariable Long id, @PathVariable Long puertaId,
                                    @ModelAttribute Puerta puerta, RedirectAttributes ra) {
        instalacionRepository.findById(id).ifPresent(inst -> {
            puerta.setId(puertaId);
            puerta.setInstalacion(inst);
            puertaRepository.save(puerta);
        });
        ra.addFlashAttribute("success", "Salón actualizado correctamente.");
        return "redirect:/instalaciones/" + id;
    }

    @PostMapping("/{id}/puerta/{puertaId}/eliminar")
    public String eliminarPuerta(@PathVariable Long id, @PathVariable Long puertaId, RedirectAttributes ra) {
        puertaRepository.deleteById(puertaId);
        ra.addFlashAttribute("success", "Salón eliminado correctamente.");
        return "redirect:/instalaciones/" + id;
    }
}
