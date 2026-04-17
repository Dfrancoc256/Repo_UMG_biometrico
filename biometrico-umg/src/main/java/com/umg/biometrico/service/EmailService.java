package com.umg.biometrico.service;

import com.umg.biometrico.model.Persona;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@SuppressWarnings("ALL")
@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final PdfService pdfService;

    @Value("${spring.mail.username}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender, PdfService pdfService) {
        this.mailSender = mailSender;
        this.pdfService = pdfService;
    }

    public void enviarCarnet(String destino, byte[] pdfBytes, String nombreArchivo) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(destino);
        helper.setSubject("Carnet enviado correctamente al correo.");
        helper.setText("Adjunto encontrar su carnet biometricoen formato PDF");

        ByteArrayResource archivoAdjunto = new ByteArrayResource(pdfBytes);
        helper.addAttachment(nombreArchivo, archivoAdjunto);
        mailSender.send(message);
    }

    @Async
    public void enviarCarnetPorCorreo(Persona persona) {
        if (persona.getCorreo() == null || persona.getCorreo().isBlank()) {
            log.warn("No se puede enviar carnet: persona {} no tiene correo registrado.", persona.getId());
            return;
        }

        log.info("Iniciando envío de carnet por correo a: {}", persona.getCorreo());

        byte[] pdfBytes;
        try {
            pdfBytes = pdfService.generarCarnetPersona(persona);
            log.debug("PDF del carnet generado correctamente para: {}", persona.getNumeroCarnet());
        } catch (Exception e) {
            log.error("Error al generar el PDF del carnet para {}: {}", persona.getNumeroCarnet(), e.getMessage(), e);
            return;
        }

        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(persona.getCorreo());
            helper.setSubject("Bienvenido/a — Tu Carné Universitario UMG");
            helper.setText(construirCuerpoHtml(persona), true);

            final byte[] adjunto = pdfBytes;
            helper.addAttachment(
                "carnet_" + persona.getNumeroCarnet() + ".pdf",
                () -> new java.io.ByteArrayInputStream(adjunto),
                "application/pdf"
            );

            mailSender.send(mensaje);
            log.info("Carnet enviado exitosamente a: {}", persona.getCorreo());

        } catch (MessagingException e) {
            log.error("Error al construir el mensaje para {}: {}", persona.getCorreo(), e.getMessage(), e);
        } catch (MailException e) {
            log.error("Error SMTP al enviar correo a {} — Verifique spring.mail.password en application.properties. Detalle: {}",
                    persona.getCorreo(), e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado al enviar correo a {}: {}", persona.getCorreo(), e.getMessage(), e);
        }
    }

    public void enviarReporteAsistencia(String destino, String nombreCurso, String fechaStr, byte[] pdfBytes)
            throws MessagingException {
        MimeMessage mensaje = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

        helper.setFrom(fromAddress);
        helper.setTo(destino);
        helper.setSubject("Reporte de Asistencia — " + nombreCurso + " — " + fechaStr);
        helper.setText(construirCuerpoReporte(nombreCurso, fechaStr), true);

        final byte[] adjunto = pdfBytes;
        helper.addAttachment(
            "asistencia_" + fechaStr + ".pdf",
            () -> new java.io.ByteArrayInputStream(adjunto),
            "application/pdf"
        );
        mailSender.send(mensaje);
    }

    private String construirCuerpoReporte(String curso, String fecha) {
        return """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;">
              <div style="background:#003366;padding:20px;text-align:center;border-radius:8px 8px 0 0;">
                <h1 style="color:#fff;margin:0;font-size:20px;">Reporte de Asistencia</h1>
                <p style="color:#cce0ff;margin:4px 0 0;">Universidad Mariano Gálvez — La Florida, Zona 19</p>
              </div>
              <div style="background:#f5f8fc;padding:24px;border:1px solid #dde3ea;">
                <p style="font-size:15px;color:#333;">Se adjunta el reporte de asistencia del curso
                  <strong>%s</strong> correspondiente a la fecha <strong>%s</strong>.</p>
                <p style="color:#777;font-size:12px;margin-top:20px;">
                  Este es un mensaje automático del Sistema Biométrico UMG.<br/>
                  Por favor no respondas a este correo.
                </p>
              </div>
              <div style="background:#003366;padding:10px;text-align:center;border-radius:0 0 8px 8px;">
                <p style="color:#aac4e8;font-size:11px;margin:0;">© 2026 Universidad Mariano Gálvez de Guatemala</p>
              </div>
            </div>
            """.formatted(curso, fecha);
    }

    private String construirCuerpoHtml(Persona persona) {
        return """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;">
              <div style="background:#003366;padding:24px;text-align:center;border-radius:8px 8px 0 0;">
                <h1 style="color:#fff;margin:0;font-size:22px;">Universidad Mariano Gálvez</h1>
                <p style="color:#cce0ff;margin:4px 0 0;">Sede La Florida, Zona 19</p>
              </div>
              <div style="background:#f5f8fc;padding:28px;border:1px solid #dde3ea;">
                <p style="font-size:16px;color:#333;">Estimado/a <strong>%s</strong>,</p>
                <p style="color:#555;">Has sido enrolado/a exitosamente en el Sistema Biométrico UMG.
                   Adjunto encontrarás tu <strong>Carné Universitario</strong> en formato PDF con firma digital.</p>
                <table style="margin:20px 0;background:#fff;border-radius:6px;padding:16px;
                              border:1px solid #dde3ea;width:100%%;">
                  <tr>
                    <td style="padding:6px 12px;color:#888;font-size:13px;">Carnet N°:</td>
                    <td style="padding:6px 12px;font-weight:bold;color:#003366;">%s</td>
                  </tr>
                  <tr>
                    <td style="padding:6px 12px;color:#888;font-size:13px;">Tipo:</td>
                    <td style="padding:6px 12px;color:#555;">%s</td>
                  </tr>
                  <tr>
                    <td style="padding:6px 12px;color:#888;font-size:13px;">Carrera:</td>
                    <td style="padding:6px 12px;color:#555;">%s</td>
                  </tr>
                  <tr>
                    <td style="padding:6px 12px;color:#888;font-size:13px;">Sección:</td>
                    <td style="padding:6px 12px;color:#555;">%s</td>
                  </tr>
                </table>
                <p style="color:#777;font-size:12px;margin-top:24px;">
                  Este es un mensaje automático. Por favor no respondas a este correo.<br/>
                  Sistema Biométrico UMG — La Florida, Zona 19
                </p>
              </div>
              <div style="background:#003366;padding:12px;text-align:center;border-radius:0 0 8px 8px;">
                <p style="color:#aac4e8;font-size:11px;margin:0;">© 2026 Universidad Mariano Gálvez de Guatemala</p>
              </div>
            </div>
            """.formatted(
                persona.getNombreCompleto(),
                persona.getNumeroCarnet() != null ? persona.getNumeroCarnet() : "—",
                persona.getTipoPersona()  != null ? persona.getTipoPersona()  : "—",
                persona.getCarrera()      != null ? persona.getCarrera()      : "—",
                persona.getSeccion()      != null ? persona.getSeccion()      : "—"
            );
    }
}
