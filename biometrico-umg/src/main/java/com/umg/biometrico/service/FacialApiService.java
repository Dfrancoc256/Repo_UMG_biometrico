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

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 🔥 URL CORRECTA DEL SERVICIO PYTHON
    private static final String FACIAL_URL = "http://127.0.0.1:5001";

    @PostConstruct
    public void iniciar() {
        if (estaDisponible()) {
            log.info("✅ Servicio facial disponible en {}", FACIAL_URL);
        } else {
            log.warn("⚠️ Servicio facial NO disponible en {}. Verifica PM2.", FACIAL_URL);
        }
    }

    @PreDestroy
    public void detener() {
        log.info("🛑 Servicio facial (PM2) se maneja externamente, no se detiene desde Java.");
    }

    public boolean estaDisponible() {
        try {
            restTemplate.getForObject(FACIAL_URL + "/estado", String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ─── ENROLAR ─────────────────────────────────────────────────────────────
    public List<Double> enrolar(Long personaId, String imagenBase64) {
        try {
            log.info("📸 Imagen recibida en enrolar: {}",
                    imagenBase64 != null ? imagenBase64.length() : "null");

            if (imagenBase64 == null || imagenBase64.trim().isEmpty()) {
                log.error("❌ Imagen base64 vacía en enrolar()");
                return null;
            }

            String imagenLimpia = limpiarBase64(imagenBase64);

            log.info("📸 Imagen limpia (enrolar): {}",
                    imagenLimpia != null ? imagenLimpia.length() : "null");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("persona_id", personaId);
            body.put("imagen_base64", imagenLimpia);

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

    // ─── VERIFICAR ───────────────────────────────────────────────────────────
    public Map<String, Object> verificar(String imagenBase64, List<Map<String, Object>> descriptores) {
        try {
            log.info("📸 Imagen recibida en verificar: {}",
                    imagenBase64 != null ? imagenBase64.length() : "null");

            if (imagenBase64 == null || imagenBase64.trim().isEmpty()) {
                log.error("❌ La imagen base64 llegó vacía a verificar()");
                return null;
            }

            String imagenLimpia = limpiarBase64(imagenBase64);

            log.info("📸 Imagen limpia enviada a Python: {}",
                    imagenLimpia != null ? imagenLimpia.length() : "null");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("imagen_base64", imagenLimpia);
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

    // ─── JSON UTILIDADES ─────────────────────────────────────────────────────
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

    // ─── LIMPIAR BASE64 ──────────────────────────────────────────────────────
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