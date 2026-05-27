package com.umg.biometrico.service;

import com.umg.biometrico.model.PasswordResetToken;
import com.umg.biometrico.model.Persona;
import com.umg.biometrico.repository.PasswordResetTokenRepository;
import com.umg.biometrico.repository.PersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PersonaRepository personaRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    public String generarToken(String correo) {

        Optional<Persona> personaOpt = personaRepository.findByCorreoIgnoreCase(correo);
        
        if (personaOpt.isEmpty()) {
            return null;
        }

        Persona persona = personaOpt.get();

        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setPersona(persona);
        resetToken.setFechaExpiracion(LocalDateTime.now().plusMinutes(15));
        resetToken.setUsado(false);

        tokenRepository.save(resetToken);

        return token;
    }

    public boolean tokenValido(String token) {

        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            return false;
        }

        PasswordResetToken resetToken = tokenOpt.get();

        return !resetToken.isUsado()
                && resetToken.getFechaExpiracion().isAfter(LocalDateTime.now());
    }

    public boolean cambiarPassword(String token, String nuevaPassword) {

        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            return false;
        }

        PasswordResetToken resetToken = tokenOpt.get();

        if (resetToken.isUsado() || resetToken.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            return false;
        }

        Persona persona = resetToken.getPersona();
        persona.setContrasena(passwordEncoder.encode(nuevaPassword));

        personaRepository.save(persona);

        resetToken.setUsado(true);
        tokenRepository.save(resetToken);

        return true;
    }
}