package com.umg.biometrico.controller;

import com.umg.biometrico.model.Carrera;
import com.umg.biometrico.repository.CarreraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/carreras")
@RequiredArgsConstructor
public class CarreraController {

    private final CarreraRepository carreraRepository;

    @GetMapping("/nueva")
    public String nueva(Model model) {

        model.addAttribute("carrera", new Carrera());
        model.addAttribute("activeMenu", "cursos");

        return "carreras/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Carrera carrera,
                          RedirectAttributes ra) {

        carreraRepository.save(carrera);

        ra.addFlashAttribute(
                "success",
                "Carrera registrada correctamente."
        );

        return "redirect:/cursos";
    }
}