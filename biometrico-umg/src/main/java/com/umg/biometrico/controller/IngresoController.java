package com.umg.biometrico.controller;

import com.umg.biometrico.model.RegistroIngreso;
import com.umg.biometrico.repository.InstalacionRepository;
import com.umg.biometrico.repository.PuertaRepository;
import com.umg.biometrico.service.RegistroIngresoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/ingreso")
@RequiredArgsConstructor
public class IngresoController {

    private final RegistroIngresoService registroIngresoService;
    private final InstalacionRepository instalacionRepository;
    private final PuertaRepository puertaRepository;

    @GetMapping
    public String formulario(Model model) {
        model.addAttribute("instalaciones", instalacionRepository.findAll());
        model.addAttribute("puertas", puertaRepository.findAll());
        model.addAttribute("activeMenu", "ingreso");
        return "ingreso/formulario";
    }

    @PostMapping("/registrar")
    public String registrar(@RequestParam Long personaId,
                            @RequestParam Long puertaId,
                            @RequestParam(defaultValue = "manual") String metodo,
                            RedirectAttributes ra) {
        registroIngresoService.registrarIngreso(personaId, puertaId, metodo);
        ra.addFlashAttribute("success", "Ingreso registrado correctamente.");
        return "redirect:/ingreso";
    }

    @PostMapping("/api/registrar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> registrarApi(@RequestBody Map<String, Object> payload) {
        try {
            Long personaId = Long.parseLong(payload.get("personaId").toString());
            Long puertaId = Long.parseLong(payload.get("puertaId").toString());
            String metodo = payload.getOrDefault("metodo", "facial").toString();
            RegistroIngreso registro = registroIngresoService.registrarIngreso(personaId, puertaId, metodo);
            return ResponseEntity.ok(Map.of("success", true, "registroId", registro.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}
