package com.umg.biometrico.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class FacialApiService {

    private Process proceso;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String FACIAL_URL = "http://127.0.0.1:5001";

    @PostConstruct
    public void iniciar() {
        if (estaDisponible()) {
            log.info(" Servicio facial disponible en {}", FACIAL_URL);
        } else {
            log.warn(" Servicio facial NO disponible en {}. Verifica PM2.", FACIAL_URL);
        }
    }

    @PreDestroy
    public void detener() {
        if (proceso != null && proceso.isAlive()) {
            proceso.destroyForcibly();
            log.info("🛑 Microservicio facial detenido.");
        }
    }

    public boolean estaDisponible() {
        try {
            restTemplate.getForObject(FACIAL_URL + "/estado", String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ─── Enrolar: extrae descriptor facial de una imagen base64 ──────────────
    public List<Double> enrolar(Long personaId, String imagenBase64) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("persona_id", personaId);
            body.put("imagen_base64", limpiarBase64(imagenBase64));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    FACIAL_URL + "/enrolar", request, Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (List<Double>) response.getBody().get("descriptor");
            }
        } catch (Exception e) {
            log.error("❌ Error al enrolar: {}", e.getMessage());
        }
        return null;
    }

    // ─── Verificar: compara imagen contra descriptores guardados ─────────────
    public Map<String, Object> verificar(String imagenBase64, List<Map<String, Object>> descriptores) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("imagen_base64", limpiarBase64(imagenBase64));
            body.put("descriptores", descriptores);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    FACIAL_URL + "/verificar", request, Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("❌ Error al verificar: {}", e.getMessage());
        }
        return null;
    }

    // ─── Convierte descriptor JSON string a lista de doubles ─────────────────
    public List<Double> descriptorDesdeJson(String json) {
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            return null;
        }
    }

    // ─── Convierte lista de doubles a JSON string para guardar en BD ─────────
    public String descriptorAJson(List<Double> descriptor) {
        try {
            return objectMapper.writeValueAsString(descriptor);
        } catch (Exception e) {
            return null;
        }
    }

    // ─── Limpia el prefijo data:image/...;base64, si existe ──────────────────
    private String limpiarBase64(String base64) {
        if (base64 != null && base64.contains(",")) {
            return base64.split(",")[1];
        }
        return base64;
    }

    public String getBaseUrl() {
        return FACIAL_URL;
    }
}