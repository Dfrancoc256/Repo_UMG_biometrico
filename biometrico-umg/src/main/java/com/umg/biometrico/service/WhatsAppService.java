package com.umg.biometrico.service;

import com.umg.biometrico.model.Persona;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class WhatsAppService {

    @Value("${whatsapp.enabled:false}")
    private boolean enabled;

    @Value("${evolution.api.url}")
    private String apiUrl;

    @Value("${evolution.api.key}")
    private String apiKey;

    @Value("${evolution.api.instance}")
    private String instance;

    private final RestTemplate restTemplate = new RestTemplate();

    // ─── Enviar mensaje de texto con datos del carnet ─────────────────────────
    @Async
    public void enviarCarnetPorWhatsApp(Persona persona) {
        if (!enabled) {
            log.info("WhatsApp deshabilitado. Active whatsapp.enabled=true en application.properties.");
            return;
        }

        if (persona.getTelefono() == null || persona.getTelefono().isBlank()) {
            log.warn("Persona {} no tiene teléfono registrado.", persona.getId());
            return;
        }

        try {
            String telefono = normalizarTelefono(persona.getTelefono());
            String mensaje   = construirMensaje(persona);
            enviarMensajeTexto(telefono, mensaje);
            log.info("✅ WhatsApp enviado a {}", telefono);

        } catch (Exception e) {
            log.error("❌ Error al enviar WhatsApp a {}: {}", persona.getTelefono(), e.getMessage());
        }
    }

    // ─── Enviar PDF como Base64 directo ──────────────────────────────────────
    public String enviarCarnetPdfUrl(String telefonoDestino, String mediaUrl, String nombreArchivo) {
        String telefono = normalizarTelefono(telefonoDestino);
        String url = apiUrl + "/message/sendMedia/" + instance;

        HttpHeaders headers = buildHeaders();

        Map<String, Object> body = new HashMap<>();
        body.put("number",    telefono);
        body.put("mediatype", "document");
        body.put("mimetype",  "application/pdf");
        body.put("media",     mediaUrl);
        body.put("caption",   "🎓 Carnet Universitario UMG");
        body.put("fileName",  nombreArchivo);

        // Headers que Evolution usará al hacer fetch de la URL
        Map<String, String> fetchHeaders = new HashMap<>();
        fetchHeaders.put("ngrok-skip-browser-warning", "true");
        body.put("headers", fetchHeaders);


        log.info("📤 Enviando PDF por URL: {}", mediaUrl);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            log.info("✅ Respuesta: {} — {}", response.getStatusCode(), response.getBody());
            return response.getBody();
        } catch (Exception e) {
            log.error("❌ Error Evolution API: {}", e.getMessage());
            throw new RuntimeException("Evolution API error: " + e.getMessage());
        }
    }

    // ─── Enviar carnet PDF como documento ────────────────────────────────────
    public String enviarCarnetPdf(String telefonoDestino, String mediaUrl) {
        String telefono = normalizarTelefono(telefonoDestino);
        String url = apiUrl + "/message/sendMedia/" + instance;

        HttpHeaders headers = buildHeaders();

        Map<String, Object> body = new HashMap<>();
        body.put("number",   telefono);
        body.put("mediatype","document");
        body.put("mimetype", "application/pdf");
        body.put("media",    mediaUrl);
        body.put("caption",  "🎓 Carnet Universitario UMG");
        body.put("fileName", "carnet-umg.pdf");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Evolution API error: " + response.getStatusCode());
        }

        log.info("✅ PDF enviado por WhatsApp a {}", telefono);
        return response.getBody();
    }

    // ─── Interno: enviar texto ────────────────────────────────────────────────
    private void enviarMensajeTexto(String telefono, String mensaje) {
        String url = apiUrl + "/message/sendText/" + instance;

        HttpHeaders headers = buildHeaders();

        Map<String, Object> body = new HashMap<>();
        body.put("number", telefono);
        body.put("text",   mensaje);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Evolution API error: " + response.getStatusCode());
        }
    }

    // ─── Headers comunes ──────────────────────────────────────────────────────
    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", apiKey);
        return headers;
    }

    // ─── Mensaje formateado ───────────────────────────────────────────────────
    private String construirMensaje(Persona persona) {
        return """
            🎓 *Universidad Mariano Gálvez de Guatemala*
            Sede La Florida, Zona 19

            Hola *%s*, has sido enrolado/a exitosamente en el Sistema Biométrico UMG.

            📋 *Datos de tu carnet:*
            • Carnet N°: `%s`
            • Tipo: %s
            • Carrera: %s

            Tu carnet en PDF ha sido enviado también a tu correo electrónico.

            _Sistema Biométrico UMG — 2026_
            """.formatted(
                persona.getNombreCompleto(),
                persona.getNumeroCarnet()  != null ? persona.getNumeroCarnet()  : "—",
                persona.getTipoPersona()   != null ? persona.getTipoPersona()   : "—",
                persona.getCarrera()       != null ? persona.getCarrera()       : "—"
        );
    }

    // ─── Normalizar teléfono guatemalteco ─────────────────────────────────────
    private String normalizarTelefono(String telefono) {
        String limpio = telefono.replaceAll("[\\s\\-()]", "");
        if (limpio.startsWith("+")) return limpio;
        if (limpio.startsWith("502")) return "+" + limpio;
        return "+502" + limpio;
    }
}