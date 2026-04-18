package com.umg.biometrico.config;

import com.umg.biometrico.model.Persona;
import com.umg.biometrico.repository.PersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Optional;

/**
 * Inyecta automáticamente el usuario autenticado en el modelo de todas las vistas,
 * disponible como ${currentUser}. Así el layout puede mostrar nombre real y foto.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final PersonaRepository personaRepository;

    @ModelAttribute("currentUser")
    public Persona currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        String correo = auth.getName();
        return personaRepository.findByCorreo(correo).orElse(null);
    }
}
