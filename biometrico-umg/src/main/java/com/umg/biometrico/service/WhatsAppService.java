package com.umg.biometrico.service;

<<<<<<< HEAD
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
public class WhatsAppService {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.whatsapp.from}")
    private String fromNumber;

    public String enviarCarnetPdf(String telefonoDestino, String mediaUrl) {

        Twilio.init(accountSid, authToken);

        Message message = Message.creator(
                        new com.twilio.type.PhoneNumber("whatsapp:" + telefonoDestino),
                        new com.twilio.type.PhoneNumber("whatsapp:" + fromNumber),
                        "Hola 👋, aquí tienes tu carnet universitario."
                )
                .setMediaUrl(java.util.List.of(URI.create(mediaUrl)))
                .create();

        return message.getSid();
    }
}
=======
import com.umg.biometrico.model.Persona;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

/**
 * Servicio para enviar el carnet por WhatsApp usando la API REST de Twilio.
 *
 * Configuración necesaria en application.properties:
 *   whatsapp.twilio.account-sid=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
 *   whatsapp.twilio.auth-token=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
 *   whatsapp.twilio.from=whatsapp:+14155238886
 *   whatsapp.enabled=true
 *
 * El número destino debe incluir código de país, ej: +50230001234
 * Para el sandbox de Twilio, el destinatario debe enviar primero
 * "join <sandbox-keyword>" al número de Twilio.
 */
@Service
@Slf4j
public class WhatsAppService {

    @Value("${whatsapp.twilio.account-sid:}")
    private String accountSid;

    @Value("${whatsapp.twilio.auth-token:}")
    private String authToken;

    @Value("${whatsapp.twilio.from:whatsapp:+14155238886}")
    private String fromNumber;

    @Value("${whatsapp.enabled:false}")
    private boolean enabled;

    private final RestTemplate restTemplate = new RestTemplate();

    @Async
    public void enviarCarnetPorWhatsApp(Persona persona) {
        if (!enabled) {
            log.info("WhatsApp deshabilitado. Para activar, configure whatsapp.enabled=true en application.properties.");
            return;
        }

        if (persona.getTelefono() == null || persona.getTelefono().isBlank()) {
            log.warn("No se puede enviar WhatsApp: persona {} no tiene teléfono registrado.", persona.getId());
            return;
        }

        if (accountSid.isBlank() || authToken.isBlank()) {
            log.error("Credenciales de Twilio no configuradas. Revise application.properties.");
            return;
        }

        try {
            String telefono = normalizarTelefono(persona.getTelefono());
            String toNumber = "whatsapp:" + telefono;

            String mensaje = construirMensaje(persona);
            enviarMensaje(toNumber, mensaje);

            log.info("Mensaje WhatsApp enviado a: {}", telefono);

        } catch (Exception e) {
            log.error("Error al enviar WhatsApp a {}: {}", persona.getTelefono(), e.getMessage());
        }
    }

    private void enviarMensaje(String to, String body) {
        String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";

        String credenciales = Base64.getEncoder().encodeToString((accountSid + ":" + authToken).getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + credenciales);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("From", fromNumber);
        params.add("To", to);
        params.add("Body", body);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Twilio respondió con código: " + response.getStatusCode());
        }
    }

    private String construirMensaje(Persona persona) {
        return """
            🎓 *Universidad Mariano Gálvez de Guatemala*
            Sede La Florida, Zona 19

            Hola *%s*, has sido enrolado/a exitosamente en el Sistema Biométrico UMG.

            📋 *Datos de tu carnet:*
            • Carnet N°: `%s`
            • Tipo: %s
            • Carrera: %s

            Tu carnet en PDF ha sido enviado a tu correo electrónico.

            _Sistema Biométrico UMG — 2026_
            """.formatted(
                persona.getNombreCompleto(),
                persona.getNumeroCarnet() != null ? persona.getNumeroCarnet() : "—",
                persona.getTipoPersona() != null ? persona.getTipoPersona() : "—",
                persona.getCarrera() != null ? persona.getCarrera() : "—"
            );
    }

    /**
     * Normaliza el teléfono: si no tiene +, agrega +502 (Guatemala por defecto).
     */
    private String normalizarTelefono(String telefono) {
        String limpio = telefono.replaceAll("[\\s\\-()]", "");
        if (limpio.startsWith("+")) {
            return limpio;
        }
        if (limpio.startsWith("502")) {
            return "+" + limpio;
        }
        return "+502" + limpio;
    }
}
>>>>>>> firmaycursos
