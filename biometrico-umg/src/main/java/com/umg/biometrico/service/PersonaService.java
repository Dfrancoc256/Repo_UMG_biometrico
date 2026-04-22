package com.umg.biometrico.service;

import com.umg.biometrico.model.Persona;
import com.umg.biometrico.repository.PersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional
public class PersonaService {

    private final PersonaRepository personaRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public List<Persona> listarTodas() {
        return personaRepository.findAll();
    }

    public List<Persona> listarActivas() {
        return personaRepository.findByActivoTrue();
    }

    public List<Persona> listarRestringidas() {
        return personaRepository.findByRestringidoTrue();
    }

    public Optional<Persona> buscarPorId(Long id) {
        return personaRepository.findById(id);
    }

    public Optional<Persona> buscarPorCorreo(String correo) {
        return personaRepository.findByCorreo(correo);
    }

    public Optional<Persona> buscarPorCarnet(String carnet) {
        return personaRepository.findByNumeroCarnet(carnet);
    }

    public List<Persona> buscar(String termino) {
        return personaRepository.buscarPersonas(termino);
    }

    public List<Persona> listarEstudiantes() {
        return personaRepository.findByRol_NombreAndActivo("ESTUDIANTE", true);
    }

    public List<Persona> listarCatedraticos() {
        return personaRepository.findByRol_NombreAndActivo("CATEDRATICO", true);
    }

    public Persona guardar(Persona persona, MultipartFile foto, String fotoBase64) throws IOException {
        if (persona.getNumeroCarnet() == null || persona.getNumeroCarnet().isBlank()) {
            persona.setNumeroCarnet(generarNumeroCarnetUnico());
        } else {
            // Validar que el carnet no esté asignado a otra persona
            personaRepository.findByNumeroCarnet(persona.getNumeroCarnet().trim())
                    .ifPresent(existente -> {
                        boolean esMismaPersona = existente.getId() != null
                                && existente.getId().equals(persona.getId());
                        if (!esMismaPersona) {
                            throw new IllegalArgumentException(
                                    "El número de carnet '" + persona.getNumeroCarnet() +
                                            "' ya está asignado a " + existente.getNombreCompleto() +
                                            ". Deje el campo vacío para generar uno automáticamente.");
                        }
                    });
            persona.setNumeroCarnet(persona.getNumeroCarnet().trim());
        }

        if (persona.getActivo() == null) {
            persona.setActivo(true);
        }

        if (persona.getRestringido() == null) {
            persona.setRestringido(false);
        }

        if (foto != null && !foto.isEmpty()) {
            persona.setFotoRuta(guardarFoto(foto));
        } else if (fotoBase64 != null && !fotoBase64.isBlank()) {
            persona.setFotoRuta(guardarFotoBase64(fotoBase64));
        }

        // Contraseña: codificar si es texto plano, preservar si está en blanco (modo edición)
        if (persona.getContrasena() != null && !persona.getContrasena().isBlank()) {
            boolean yaEsBcrypt = persona.getContrasena().startsWith("$2a$")
                    || persona.getContrasena().startsWith("$2b$")
                    || persona.getContrasena().startsWith("$2y$");
            if (!yaEsBcrypt) {
                persona.setContrasena(passwordEncoder.encode(persona.getContrasena()));
            }
        } else if (persona.getId() != null) {
            // Editando: no cambiar la contraseña si se dejó en blanco
            personaRepository.findById(persona.getId())
                    .ifPresent(existente -> persona.setContrasena(existente.getContrasena()));
        } else {
            persona.setContrasena(null);
        }

        return personaRepository.save(persona);
    }

    public Persona actualizar(Persona persona) {
        return personaRepository.save(persona);
    }

    public void eliminar(Long id) {
        personaRepository.findById(id).ifPresent(p -> {
            p.setActivo(false);
            personaRepository.save(p);
        });
    }

    public void restringir(Long id, String motivo) {
        personaRepository.findById(id).ifPresent(p -> {
            p.setRestringido(true);
            p.setMotivoRestriccion(motivo);
            personaRepository.save(p);
        });
    }

    public void levantarRestriccion(Long id) {
        personaRepository.findById(id).ifPresent(p -> {
            p.setRestringido(false);
            p.setMotivoRestriccion(null);
            personaRepository.save(p);
        });
    }

    private String generarNumeroCarnetUnico() {
        String carnet;
        do {
            // Combina timestamp + random para garantizar unicidad
            long suffix = System.currentTimeMillis() % 10_000_000L
                    + ThreadLocalRandom.current().nextInt(1000);
            carnet = String.format("UMG-%07d", suffix % 10_000_000L);
        } while (personaRepository.findByNumeroCarnet(carnet).isPresent());
        return carnet;
    }

    private String guardarFotoBase64(String base64Data) throws IOException {
        String datos = base64Data.contains(",") ? base64Data.split(",")[1] : base64Data;
        byte[] imageBytes = Base64.getDecoder().decode(datos);

        Path dirPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        String nombreArchivo = UUID.randomUUID() + "_webcam.jpg";
        Files.write(dirPath.resolve(nombreArchivo), imageBytes);
        return "uploads/fotos/" + nombreArchivo;
    }

    private String guardarFoto(MultipartFile foto) throws IOException {
        Path dirPath = Paths.get(uploadDir).toAbsolutePath().normalize();

        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        String nombreArchivo = UUID.randomUUID() + "_" + foto.getOriginalFilename();
        Path rutaCompleta = dirPath.resolve(nombreArchivo);

        foto.transferTo(rutaCompleta.toFile());

        return "uploads/fotos/" + nombreArchivo;
    }

    public List<Persona> listarUltimas5() {
        return personaRepository.findTop5ByActivoTrueOrderByIdDesc();
    }

    public Long contarPorTipo(String tipo) {
        return personaRepository.contarPorRol(tipo);
    }

    public Long contarActivos() {
        return personaRepository.contarActivos();
    }

    public Long contarRestringidos() {
        return personaRepository.contarRestringidos();
    }
}