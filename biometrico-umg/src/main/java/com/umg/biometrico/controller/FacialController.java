package com.umg.biometrico.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umg.biometrico.model.Persona;
import com.umg.biometrico.service.FacialApiService;
import com.umg.biometrico.service.PersonaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/facial")
@RequiredArgsConstructor
@Slf4j
public class FacialController {

    private final FacialApiService facialApiService;
    private final PersonaService personaService;
    private final ObjectMapper objectMapper;

    // ─── Enrolar rostro de una persona existente ──────────────────────────────
    @PostMapping("/enrolar/{id}")
    public ResponseEntity<Map<String, Object>> enrolar(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        Map<String, Object> resp = new HashMap<>();
        try {
            Persona persona = personaService.buscarPorId(id)
                    .orElseThrow(() -> new RuntimeException("Persona no encontrada"));

            String imagenBase64 = body.get("imagen");
            if (imagenBase64 == null || imagenBase64.isBlank()) {
                resp.put("ok", false);
                resp.put("mensaje", "No se recibió imagen");
                return ResponseEntity.badRequest().body(resp);
            }

            List<Double> descriptor = facialApiService.enrolar(id, imagenBase64);
            if (descriptor == null) {
                resp.put("ok", false);
                resp.put("mensaje", "No se detectó rostro en la imagen. Intenta de nuevo.");
                return ResponseEntity.ok(resp);
            }

            persona.setEncodingFacial(facialApiService.descriptorAJson(descriptor));
            personaService.actualizar(persona);

            resp.put("ok", true);
            resp.put("mensaje", "✅ Rostro enrolado correctamente para " + persona.getNombreCompleto());
            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            log.error("Error al enrolar: {}", e.getMessage());
            resp.put("ok", false);
            resp.put("mensaje", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(resp);
        }
    }

    // ─── Verificar rostro contra todos los enrolados ──────────────────────────
    @PostMapping("/verificar")
    public ResponseEntity<Map<String, Object>> verificar(
            @RequestBody Map<String, String> body) {

        Map<String, Object> resp = new HashMap<>();
        try {
            String imagenBase64 = body.get("imagen");
            if (imagenBase64 == null || imagenBase64.isBlank()) {
                resp.put("ok", false);
                resp.put("mensaje", "No se recibió imagen");
                return ResponseEntity.badRequest().body(resp);
            }

            // Cargar todos los descriptores de la BD
            List<Persona> personas = personaService.listarActivas();
            List<Map<String, Object>> descriptores = new ArrayList<>();

            for (Persona p : personas) {
                if (p.getEncodingFacial() != null) {
                    List<Double> descriptor = facialApiService.descriptorDesdeJson(p.getEncodingFacial());
                    if (descriptor != null) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("persona_id", p.getId());
                        item.put("descriptor", descriptor);
                        descriptores.add(item);
                    }
                }
            }

            if (descriptores.isEmpty()) {
                resp.put("ok", false);
                resp.put("mensaje", "No hay personas enroladas en el sistema");
                return ResponseEntity.ok(resp);
            }

            Map<String, Object> resultado = facialApiService.verificar(imagenBase64, descriptores);
            if (resultado == null) {
                resp.put("ok", false);
                resp.put("mensaje", "Error al procesar la imagen");
                return ResponseEntity.ok(resp);
            }

            boolean reconocido = Boolean.TRUE.equals(resultado.get("reconocido"));
            if (reconocido) {
                Long personaId = Long.valueOf(resultado.get("persona_id").toString());
                Persona persona = personaService.buscarPorId(personaId).orElse(null);
                if (persona != null) {
                    resp.put("ok", true);
                    resp.put("reconocido", true);
                    resp.put("persona_id", personaId);
                    resp.put("nombre", persona.getNombreCompleto());
                    resp.put("carnet", persona.getNumeroCarnet());
                    resp.put("tipo", persona.getTipoPersona());
                    resp.put("confianza", resultado.get("confianza"));
                    return ResponseEntity.ok(resp);
                }
            }

            resp.put("ok", true);
            resp.put("reconocido", false);
            resp.put("mensaje", "Rostro no reconocido");
            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            log.error("Error al verificar: {}", e.getMessage());
            resp.put("ok", false);
            resp.put("mensaje", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(resp);
        }
    }

    // ─── Estado del servicio facial ───────────────────────────────────────────
    @GetMapping("/estado")
    public ResponseEntity<Map<String, Object>> estado() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("disponible", facialApiService.estaDisponible());
        return ResponseEntity.ok(resp);
    }
}