package com.umg.biometrico.config;

import com.umg.biometrico.model.Persona;
import com.umg.biometrico.repository.PersonaRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Component
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final PersonaRepository personaRepository;

    public CustomLoginSuccessHandler(PersonaRepository personaRepository) {
        this.personaRepository = personaRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        boolean esAdmin = authorities.stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        boolean esCatedratico = authorities.stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_CATEDRATICO"));

        boolean esEstudiante = authorities.stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ESTUDIANTE"));

        if (esAdmin) {
            response.sendRedirect("/dashboard");
            return;
        }

        if (esCatedratico) {
            response.sendRedirect("/cursos");
            return;
        }

        if (esEstudiante) {
            String correo = authentication.getName();

            Persona persona = personaRepository.findByCorreo(correo)
                    .orElseThrow(() -> new IOException("No se encontró la persona autenticada"));

            response.sendRedirect("/personas/" + persona.getId() + "/ver");
            return;
        }

        response.sendRedirect("/acceso-denegado");
    }
}