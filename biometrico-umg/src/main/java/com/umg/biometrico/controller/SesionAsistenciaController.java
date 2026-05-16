package com.umg.biometrico.controller;

import com.umg.biometrico.model.Persona;
import com.umg.biometrico.model.SesionAsistencia;
import com.umg.biometrico.repository.CamaraRepository;
import com.umg.biometrico.repository.InstalacionRepository;
import com.umg.biometrico.repository.PersonaRepository;
import com.umg.biometrico.service.CursoService;
import com.umg.biometrico.service.SesionAsistenciaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@Controller
@RequestMapping("/asistencia/sesion")
@RequiredArgsConstructor
public class SesionAsistenciaController {

    private final SesionAsistenciaService sesionService;
    private final CursoService cursoService;
    private final PersonaRepository personaRepository;
    private final InstalacionRepository instalacionRepository;
    private final CamaraRepository camaraRepository;

    @GetMapping("/habilitar")
    public String vistaHabilitar(Model model, Principal principal) {

        Persona catedratico = personaRepository.findByCorreo(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        model.addAttribute("catedratico", catedratico);
        model.addAttribute("cursos", cursoService.listarPorCatedratico(catedratico.getId()));
        model.addAttribute("instalaciones", instalacionRepository.findAll());
        model.addAttribute("camaras", camaraRepository.findByActivaTrue());
        model.addAttribute("activeMenu", "asistencia");

        return "asistencia/habilitar";
    }

    @PostMapping("/api/habilitar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> habilitar(@RequestBody Map<String, Object> body,
                                                         Principal principal) {
        try {
            Persona catedratico = personaRepository.findByCorreo(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            Long cursoId = Long.parseLong(body.get("cursoId").toString());
            Long puertaId = Long.parseLong(body.get("puertaId").toString());
            Long camaraId = Long.parseLong(body.get("camaraId").toString());

            SesionAsistencia sesion = sesionService.habilitarSesion(
                    cursoId,
                    catedratico.getId(),
                    puertaId,
                    camaraId
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "sesionId", sesion.getId(),
                    "curso", sesion.getCurso().getNombre(),
                    "camara", sesion.getCamara().getNombre()
            ));

        } catch (Exception e) {
            String mensaje = e.getMessage();

            if (mensaje == null || mensaje.contains("could not execute statement") || mensaje.contains("duplicate key")) {
                mensaje = "Esta cámara ya tiene una sesión activa. Intente con otra cámara o espere a que finalice.";
            }

            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", mensaje
            ));
        }
    }

    @PostMapping("/api/finalizar/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> finalizar(@PathVariable Long id) {
        try {
            SesionAsistencia sesion = sesionService.finalizarSesion(id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "sesionId", sesion.getId()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/api/activa/camara/{camaraId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sesionActivaPorCamara(@PathVariable Long camaraId) {
        try {
            SesionAsistencia sesion = sesionService.obtenerSesionActivaPorCamara(camaraId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "sesionId", sesion.getId(),
                    "cursoId", sesion.getCurso().getId(),
                    "curso", sesion.getCurso().getNombre(),
                    "catedratico", sesion.getCatedratico().getNombreCompleto(),
                    "puertaId", sesion.getPuerta().getId(),
                    "puerta", sesion.getPuerta().getNombre(),
                    "camaraId", sesion.getCamara().getId(),
                    "camara", sesion.getCamara().getNombre()
            ));

        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/api/finalizar-activa/puerta/{puertaId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> finalizarActivaPorPuerta(@PathVariable Long puertaId) {
        try {
            SesionAsistencia sesion = sesionService.finalizarSesionActivaPorPuerta(puertaId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "sesionId", sesion.getId(),
                    "mensaje", "Sesión activa cerrada correctamente."
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
    
}