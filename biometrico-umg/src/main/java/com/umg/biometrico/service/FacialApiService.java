package com.umg.biometrico.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class FacialApiService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ✅ Lee desde application.properties
    @Value("${facial.api.url}")
    private String facialUrl;

    public boolean estaDisponible() {
        try {
            restTemplate.getForObject(facialUrl + "/estado", String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ─── ENROLAR ─────────────────────────────────────────────────────────────
    public List<Double> enrolar(Long personaId, String imagenBase64) {
        try {
            if (imagenBase64 == null || imagenBase64.trim().isEmpty()) {
                log.error("❌ Imagen base64 vacía en enrolar()");
                return null;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("persona_id",     personaId);
            body.put("imagen_base64",  limpiarBase64(imagenBase64));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    facialUrl + "/enrolar", request, Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (List<Double>) response.getBody().get("descriptor");
            }

        } catch (Exception e) {
            log.error("❌ Error al enrolar: {}", e.getMessage());
        }
        return null;
    }

    // ─── VERIFICAR 1:1 ───────────────────────────────────────────────────────
    public Map<String, Object> verificar1a1(String imagenActualBase64, String imagenGuardadaBase64) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("imagen_actual_base64",   limpiarBase64(imagenActualBase64));
            body.put("imagen_guardada_base64", limpiarBase64(imagenGuardadaBase64));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    facialUrl + "/verificar-1a1", request, Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("❌ Error en verificar1a1: {}", e.getMessage());
        }
        return null;
    }

    // ─── VERIFICAR 1:N ───────────────────────────────────────────────────────
    public Map<String, Object> verificar(String imagenBase64, List<Map<String, Object>> descriptores) {
        try {
            if (imagenBase64 == null || imagenBase64.trim().isEmpty()) {
                log.error("❌ Imagen base64 vacía en verificar()");
                return null;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("imagen_base64", limpiarBase64(imagenBase64));
            body.put("descriptores",  descriptores);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    facialUrl + "/verificar", request, Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }

        } catch (Exception e) {
            log.error("❌ Error al verificar: {}", e.getMessage());
        }
        return null;
    }

    // ─── UTILIDADES ──────────────────────────────────────────────────────────
    public List<Double> descriptorDesdeJson(String json) {
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            log.error("❌ Error al convertir JSON a descriptor: {}", e.getMessage());
            return null;
        }
    }

    public String descriptorAJson(List<Double> descriptor) {
        try {
            return objectMapper.writeValueAsString(descriptor);
        } catch (Exception e) {
            log.error("❌ Error al convertir descriptor a JSON: {}", e.getMessage());
            return null;
        }
    }

    private String limpiarBase64(String base64) {
        if (base64 != null && base64.contains(",")) {
            return base64.split(",")[1];
        }
        return base64;
    }

    public String getBaseUrl() {
        return facialUrl;
    }

    public Map<String, Object> verificarPersona(String imagenBase64, List<Double> descriptorGuardado) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("imagen_base64", limpiarBase64(imagenBase64));
            body.put("descriptor",    descriptorGuardado);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    facialUrl + "/verificar-persona", request, Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("❌ Error en verificarPersona: {}", e.getMessage());
        }
        return null;
    }
}