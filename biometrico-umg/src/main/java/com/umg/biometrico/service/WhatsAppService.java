package com.umg.biometrico.service;

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