package com.umg.biometrico.config;

import com.umg.biometrico.model.Persona;
import com.umg.biometrico.repository.PersonaRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomLoginSuccessHandler customLoginSuccessHandler;

    public SecurityConfig(CustomLoginSuccessHandler customLoginSuccessHandler) {
        this.customLoginSuccessHandler = customLoginSuccessHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // ── Público ─────────────────────────────────────────────
                        .requestMatchers(
                                "/auth/**", "/css/**", "/js/**", "/img/**",
                                "/webjars/**", "/uploads/**", "/fotos_personas/**",
                                "/personas/*/carnet-publico", "/acceso-denegado", "/verificar/**"
                        ).permitAll()

                        // ── Solo ADMIN ───────────────────────────────────────────
                        .requestMatchers(
                                "/dashboard",
                                "/personas",
                                "/personas/nuevo", "/personas/guardar", "/personas/*/editar",
                                "/personas/*/eliminar",
                                "/personas/*/levantar-restriccion",
                                "/personas/*/restringir",
                                "/personas/restringidos",
                                "/instalaciones/nueva", "/instalaciones/guardar",
                                "/instalaciones/*/puerta/nueva", "/instalaciones/*/puerta/guardar",
                                "/cursos/nuevo", "/cursos/guardar", "/cursos/*/editar",
                                "/reportes/**"
                        ).hasRole("ADMIN")

                        // ── ADMIN + CATEDRÁTICO ──────────────────────────────────
                        .requestMatchers(
                                "/cursos",
                                "/asistencia", "/asistencia/**",
                                "/ingreso/**",
                                "/instalaciones",
                                "/cursos/*/inscribir", "/cursos/*/desinscribir",
                                "/cursos/*/asignar-seccion", "/cursos/*/quitar-seccion"
                        ).hasAnyRole("ADMIN", "CATEDRATICO")

                        // ── ADMIN + ESTUDIANTE ───────────────────────────────────
                        .requestMatchers(
                                "/personas/*/ver"
                        ).hasAnyRole("ADMIN", "ESTUDIANTE")

                        // ── ESTUDIANTE ────────────────────────────────────────────
                        .requestMatchers(
                                "/mis-cursos", "/mi-asistencia"
                        ).hasRole("ESTUDIANTE")

                        // ── Cualquier usuario autenticado ────────────────────────
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/acceso-denegado")
                )
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/auth/login")
                        .successHandler(customLoginSuccessHandler)
                        .failureUrl("/auth/login?error=true")
                        .usernameParameter("correo")
                        .passwordParameter("contrasena")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/auth/logout"))
                        .logoutSuccessUrl("/auth/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(PersonaRepository personaRepository) {
        return correo -> {
            Persona persona = personaRepository.findByCorreo(correo)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + correo));

            if (!Boolean.TRUE.equals(persona.getActivo())) {
                throw new UsernameNotFoundException("Cuenta desactivada");
            }

            if (persona.getRol() == null || persona.getRol().getNombre() == null) {
                throw new UsernameNotFoundException("El usuario no tiene un rol asignado");
            }

            String rol = "ROLE_" + persona.getRol().getNombre().toUpperCase();

            return new User(
                    persona.getCorreo(),
                    persona.getContrasena() != null ? persona.getContrasena() : "",
                    List.of(new SimpleGrantedAuthority(rol))
            );
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}