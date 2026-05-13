package com.umg.biometrico.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umg.biometrico.model.Persona;
import com.umg.biometrico.service.FacialApiService;
import com.umg.biometrico.service.PersonaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    @PostMapping("/enrolar/{id}")
    public ResponseEntity<Map<String, Object>> enrolar(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        Map<String, Object> resp = new HashMap<>();

        try {
            Persona persona = personaService.buscarPorId(id)
                    .orElseThrow(() -> new RuntimeException("Persona no encontrada"));

            String imagenBase64 = body.get("imagen");

            log.info("📸 Imagen recibida en enrolar controller: {}",
                    imagenBase64 != null ? imagenBase64.length() : "null");

            if (imagenBase64 == null || imagenBase64.isBlank()) {
                resp.put("ok", false);
                resp.put("mensaje", "No se recibió imagen");
                return ResponseEntity.badRequest().body(resp);
            }

            if (!imagenBase64.startsWith("data:image")) {
                resp.put("ok", false);
                resp.put("mensaje", "Formato de imagen inválido");
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
            resp.put("mensaje", "Rostro enrolado correctamente para " + persona.getNombreCompleto());
            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            log.error("❌ Error al enrolar: {}", e.getMessage());

            resp.put("ok", false);
            resp.put("mensaje", "Error: " + e.getMessage());

            return ResponseEntity.internalServerError().body(resp);
        }
    }

    @PostMapping("/verificar")
    public ResponseEntity<Map<String, Object>> verificar(
            @RequestBody Map<String, String> body) {

        Map<String, Object> resp = new HashMap<>();

        try {
            String imagenBase64 = body.get("imagen");

            log.info("📸 Imagen recibida en verificar controller: {}",
                    imagenBase64 != null ? imagenBase64.length() : "null");

            if (imagenBase64 == null || imagenBase64.isBlank()) {
                resp.put("ok", false);
                resp.put("mensaje", "No se recibió imagen");
                return ResponseEntity.badRequest().body(resp);
            }

            if (!imagenBase64.startsWith("data:image")) {
                log.error("❌ Imagen no tiene formato base64 válido");
                resp.put("ok", false);
                resp.put("mensaje", "Formato de imagen inválido");
                return ResponseEntity.badRequest().body(resp);
            }

            List<Persona> personas = personaService.listarActivas();
            List<Map<String, Object>> descriptores = new ArrayList<>();

            for (Persona p : personas) {
                if (p.getEncodingFacial() != null && !p.getEncodingFacial().isBlank()) {
                    List<Double> descriptor = facialApiService.descriptorDesdeJson(p.getEncodingFacial());

                    if (descriptor != null && !descriptor.isEmpty()) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("persona_id", p.getId());
                        item.put("descriptor", descriptor);
                        descriptores.add(item);
                    }
                }
            }

            log.info("🧠 Descriptores cargados: {}", descriptores.size());

            if (descriptores.isEmpty()) {
                resp.put("ok", false);
                resp.put("mensaje", "No hay personas enroladas en el sistema");
                return ResponseEntity.ok(resp);
            }

            Map<String, Object> resultado = facialApiService.verificar(imagenBase64, descriptores);

            if (resultado == null) {
                log.error("❌ Python devolvió NULL");
                resp.put("ok", false);
                resp.put("mensaje", "Error al procesar la imagen en Python");
                return ResponseEntity.ok(resp);
            }

            log.info("🧠 Resultado Python: {}", resultado);

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
            log.error("❌ Error al verificar: {}", e.getMessage());

            resp.put("ok", false);
            resp.put("mensaje", "Error: " + e.getMessage());

            return ResponseEntity.internalServerError().body(resp);
        }
    }

    @GetMapping("/estado")
    public ResponseEntity<Map<String, Object>> estado() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("disponible", facialApiService.estaDisponible());
        resp.put("url", facialApiService.getBaseUrl());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/verificar-1a1")
    public ResponseEntity<Map<String, Object>> verificar1a1(
            @RequestBody Map<String, String> body) {

        Map<String, Object> resp = new HashMap<>();
        try {
            String carnet = body.get("carnet");
            String imagen = body.get("imagen");

            if (carnet == null || imagen == null) {
                resp.put("ok", false);
                resp.put("mensaje", "Faltan datos");
                return ResponseEntity.badRequest().body(resp);
            }

            Persona persona = personaService.buscarPorCarnet(carnet).orElse(null);
            if (persona == null) {
                resp.put("ok", false);
                resp.put("mensaje", "No se encontró persona con ese carnet");
                return ResponseEntity.ok(resp);
            }

            if (persona.getFotoRuta() == null) {
                resp.put("ok", false);
                resp.put("mensaje", "La persona no tiene foto registrada");
                return ResponseEntity.ok(resp);
            }

            // Leer foto guardada y convertir a base64
            Path rutaFoto = Paths.get(persona.getFotoRuta()).isAbsolute()
                    ? Paths.get(persona.getFotoRuta())
                    : Paths.get("").toAbsolutePath().resolve(persona.getFotoRuta());

            if (!Files.exists(rutaFoto)) {
                resp.put("ok", false);
                resp.put("mensaje", "No se encontró la foto en el servidor");
                return ResponseEntity.ok(resp);
            }

            byte[] fotoBytes = Files.readAllBytes(rutaFoto);
            String fotoBase64 = java.util.Base64.getEncoder().encodeToString(fotoBytes);

            Map<String, Object> resultado = facialApiService.verificar1a1(imagen, fotoBase64);
            if (resultado == null) {
                resp.put("ok", false);
                resp.put("mensaje", "Error al procesar la imagen");
                return ResponseEntity.ok(resp);
            }

            boolean coincide = Boolean.TRUE.equals(resultado.get("coincide"));
            resp.put("ok", true);
            resp.put("coincide", coincide);
            resp.put("confianza", resultado.get("confianza"));
            resp.put("persona_id", persona.getId());
            resp.put("nombre", persona.getNombreCompleto());
            resp.put("carnet", persona.getNumeroCarnet());
            resp.put("tipo", persona.getTipoPersona());
            resp.put("fotoUrl", "/" + persona.getFotoRuta());

            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            log.error("Error en verificar1a1: {}", e.getMessage());
            resp.put("ok", false);
            resp.put("mensaje", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(resp);
        }
    }
}