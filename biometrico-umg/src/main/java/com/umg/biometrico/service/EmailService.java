package com.umg.biometrico.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void enviarCarnet(String destino, byte[] pdfBytes, String nombreArchivo) throws MessagingException {
        MimeMessage mensaje = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mensaje, true);

        helper.setTo(destino);
        helper.setSubject("Carnet Biométrico UMG");
        helper.setText("Adjunto encontrará su carnet biométrico en formato PDF.");

        ByteArrayResource archivoAdjunto = new ByteArrayResource(pdfBytes);
        helper.addAttachment(nombreArchivo, archivoAdjunto);

        mailSender.send(mensaje);
    }
}