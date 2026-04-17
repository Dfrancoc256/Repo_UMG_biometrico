package com.umg.biometrico.controller;

import com.umg.biometrico.model.Persona;
import com.umg.biometrico.repository.PersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class VerificacionController {

    private final PersonaRepository personaRepository;

    @GetMapping("/verificar/{carnet}")
    public String verificarCarnet(@PathVariable String carnet, Model model) {

        Persona persona = personaRepository.findByNumeroCarnet(carnet).orElse(null);

        if (persona == null) {
            model.addAttribute("mensaje", "No se encontró ninguna persona con el carnet: " + carnet);
            return "verificacion/error";
        }

        model.addAttribute("persona", persona);
        return "verificacion/resultado";
    }
}
