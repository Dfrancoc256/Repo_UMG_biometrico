package com.umg.biometrico.controller;

import com.umg.biometrico.dto.DashboardDTO;
import com.umg.biometrico.service.DashboardService;
import com.umg.biometrico.service.PersonaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final PersonaService personaService;

    @GetMapping
    public String dashboard(Model model) {
        DashboardDTO stats = dashboardService.obtenerEstadisticas();
        model.addAttribute("stats", stats);
        model.addAttribute("fechaActual",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        model.addAttribute("ultimosEstudiantes", personaService.listarUltimas5());
        model.addAttribute("activeMenu", "dashboard");
        return "dashboard/index";
    }
}
