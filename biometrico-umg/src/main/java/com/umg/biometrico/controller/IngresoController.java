package com.umg.biometrico.controller;

import com.umg.biometrico.service.CursoService;
import com.umg.biometrico.repository.CursoRepository;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors; // ← este faltaba

@Controller
@RequestMapping("/ingreso")
@RequiredArgsConstructor
public class IngresoController {

    private final RegistroIngresoService registroIngresoService;
    private final InstalacionRepository instalacionRepository;
    private final PuertaRepository puertaRepository;
    private final CursoService cursoService;

    @GetMapping
    public String formulario(Model model) {
        model.addAttribute("instalaciones", instalacionRepository.findAll());
        model.addAttribute("cursos", cursoService.listarActivos());
        model.addAttribute("activeMenu", "ingreso");
        return "ingreso/formulario";
    }

    @GetMapping("/api/recientes")
    @ResponseBody
    public List<Map<String, Object>> recientes(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) Long puertaId) {
        return registroIngresoService.obtenerRecientes(limit, puertaId);
    }

    @GetMapping("/api/puertas-todas")
    @ResponseBody
    public List<Map<String, Object>> puertas() {
        return puertaRepository.findAll().stream()
                .map(p -> Map.<String, Object>of("id", p.getId(), "nombre", p.getNombre()))
                .collect(Collectors.toList()); // ← ahora funciona
    }

    @GetMapping("/api/puertas")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> puertasPorInstalacion(
            @RequestParam Long instalacionId) {
        List<Map<String, Object>> result = puertaRepository
                .findByInstalacionId(instalacionId).stream()
                .map(p -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id",     p.getId());
                    m.put("nombre", p.getNombre() != null ? p.getNombre() : "Puerta " + p.getId());
                    return m;
                }).toList();
        return ResponseEntity.ok(result);
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
            Long puertaId  = Long.parseLong(payload.get("puertaId").toString());
            String metodo  = payload.getOrDefault("metodo", "facial").toString();
            Long cursoId   = payload.get("cursoId") != null
                    ? Long.parseLong(payload.get("cursoId").toString())
                    : null;

            RegistroIngreso registro = registroIngresoService.registrarIngreso(
                    personaId, puertaId, metodo, cursoId
            );
            return ResponseEntity.ok(Map.of(
                    "success",   true,
                    "registroId", registro.getId(),
                    "fechaHora", registro.getFechaHora().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}