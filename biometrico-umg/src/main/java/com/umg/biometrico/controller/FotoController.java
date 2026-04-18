package com.umg.biometrico.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Sirve todos los archivos bajo /uploads/** desde el directorio "uploads"
 * relativo al directorio de trabajo del servidor (mismo punto de partida que
 * WebConfig y PersonaService). Usa HttpServletRequest para evitar cualquier
 * problema de codificación de path variables.
 */
@RestController
@Slf4j
public class FotoController {

    // Mismo directorio raíz que WebConfig y que PersonaService usan para guardar
    private final Path uploadsRoot = Paths.get("uploads").toAbsolutePath().normalize();

    @GetMapping("/uploads/**")
    public ResponseEntity<Resource> servirArchivo(HttpServletRequest request) {
        try {
            // Decodificar URI y extraer la parte después de /uploads/
            String uri = URLDecoder.decode(request.getRequestURI(), StandardCharsets.UTF_8);
            int idx = uri.indexOf("/uploads/");
            if (idx < 0) {
                return ResponseEntity.notFound().build();
            }
            String subPath = uri.substring(idx + "/uploads/".length());

            // Resolver ruta completa y verificar que esté dentro de uploadsRoot
            Path filePath = uploadsRoot.resolve(subPath).normalize();
            if (!filePath.startsWith(uploadsRoot)) {
                log.warn("[Fotos] Intento de path traversal bloqueado: {}", subPath);
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                String nombre = filePath.getFileName().toString().toLowerCase();
                String ct = nombre.endsWith(".png") ? "image/png"
                          : nombre.endsWith(".gif") ? "image/gif"
                          : "image/jpeg";
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(ct))
                        .header(HttpHeaders.CACHE_CONTROL, "max-age=86400, public")
                        .body(resource);
            }

            log.debug("[Fotos] Archivo no encontrado: {} (uploadsRoot={})", filePath, uploadsRoot);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("[Fotos] Error sirviendo archivo: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // Compatibilidad con rutas antiguas almacenadas como fotos_personas/xxx
    @GetMapping("/fotos_personas/**")
    public ResponseEntity<Resource> servirFotoLegacy(HttpServletRequest request) {
        try {
            Path legacyRoot = Paths.get("fotos_personas").toAbsolutePath().normalize();
            String uri = URLDecoder.decode(request.getRequestURI(), StandardCharsets.UTF_8);
            int idx = uri.indexOf("/fotos_personas/");
            if (idx < 0) return ResponseEntity.notFound().build();

            String subPath = uri.substring(idx + "/fotos_personas/".length());
            Path filePath = legacyRoot.resolve(subPath).normalize();

            if (!filePath.startsWith(legacyRoot)) {
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .header(HttpHeaders.CACHE_CONTROL, "max-age=86400, public")
                        .body(resource);
            }
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
