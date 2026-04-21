package com.umg.biometrico.controller;

import com.umg.biometrico.service.PersonaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/facial")
@RequiredArgsConstructor
public class FacialViewController {

    private final PersonaService personaService;

    @GetMapping("/enrolar")
    public String enrolar(Model model) {
        model.addAttribute("personas", personaService.listarActivas());
        model.addAttribute("activeMenu", "facial");
        return "facial/enrolar";
    }

    @GetMapping("/asistencia")
    public String asistencia(Model model) {
        model.addAttribute("activeMenu", "facial");
        return "facial/asistencia";
    }
}