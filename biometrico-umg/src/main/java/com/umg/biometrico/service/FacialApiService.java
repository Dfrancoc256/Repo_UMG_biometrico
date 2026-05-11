package com.umg.biometrico.service;

import com.fasterxml.jackson.core.type.TypeReference;
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

    public List<Double> enrolar(Long personaId, String imagenBase64) {
        try {
            if (imagenBase64 == null || imagenBase64.trim().isEmpty()) {
                log.error("❌ Imagen base64 vacía en enrolar()");
                return null;
            }

            String imagenLimpia = limpiarBase64(imagenBase64);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("persona_id", personaId);
            body.put("imagen_base64", imagenLimpia);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    FACIAL_URL + "/enrolar",
                    request,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object descriptor = response.getBody().get("descriptor");

                return objectMapper.convertValue(
                        descriptor,
                        new TypeReference<List<Double>>() {}
                );
            }

        } catch (Exception e) {
            log.error("❌ Error al enrolar: {}", e.getMessage());
        }

        return null;
    }

    public Map<String, Object> verificar(String imagenBase64, List<Map<String, Object>> descriptores) {
        try {
            if (imagenBase64 == null || imagenBase64.trim().isEmpty()) {
                log.error("❌ La imagen base64 llegó vacía a verificar()");
                return null;
            }

            String imagenLimpia = limpiarBase64(imagenBase64);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("imagen_base64", imagenLimpia);
            body.put("descriptores", descriptores);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    FACIAL_URL + "/verificar",
                    request,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }

        } catch (Exception e) {
            log.error("❌ Error al verificar: {}", e.getMessage());
        }

        return null;
    }

    public Map<String, Object> verificarPersona(String imagenBase64, List<Double> descriptorGuardado) {
        try {
            if (imagenBase64 == null || imagenBase64.trim().isEmpty()) {
                log.error("❌ La imagen base64 llegó vacía a verificarPersona()");
                return null;
            }

            if (descriptorGuardado == null || descriptorGuardado.isEmpty()) {
                log.error("❌ Descriptor guardado vacío en verificarPersona()");
                return null;
            }

            String imagenLimpia = limpiarBase64(imagenBase64);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("imagen_base64", imagenLimpia);
            body.put("descriptor", descriptorGuardado);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    FACIAL_URL + "/verificar-persona",
                    request,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }

        } catch (Exception e) {
            log.error("❌ Error al verificar persona: {}", e.getMessage());
        }

        return null;
    }

    public List<Double> descriptorDesdeJson(String json) {
        try {
            return objectMapper.readValue(
                    json,
                    new TypeReference<List<Double>>() {}
            );
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
        return FACIAL_URL;
    }
}