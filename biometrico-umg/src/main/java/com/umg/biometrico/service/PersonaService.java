package com.umg.biometrico.service;

import com.umg.biometrico.model.Persona;
import com.umg.biometrico.repository.PersonaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PersonaService {

    private final PersonaRepository personaRepository;
    private final PasswordEncoder passwordEncoder;
    private final FacialApiService facialApiService;

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

        Persona entidad;

        if (persona.getId() != null) {
            entidad = personaRepository.findById(persona.getId())
                    .orElseThrow(() -> new RuntimeException("Persona no encontrada"));

            entidad.setNombre(persona.getNombre());
            entidad.setApellido(persona.getApellido());
            entidad.setTelefono(persona.getTelefono());
            entidad.setCorreo(persona.getCorreo());
            entidad.setTipoPersona(persona.getTipoPersona());
            entidad.setCarrera(persona.getCarrera());
            entidad.setSeccion(persona.getSeccion());

            if (persona.getNumeroCarnet() != null
                    && !persona.getNumeroCarnet().isBlank()
                    && !persona.getNumeroCarnet().equals("UMG-XXXXXXX")) {
                entidad.setNumeroCarnet(persona.getNumeroCarnet());
            }

            if (persona.getRol() != null && persona.getRol().getId() != null) {
                entidad.setRol(persona.getRol());
            }

        } else {
            entidad = persona;
        }

        if (entidad.getNumeroCarnet() == null
                || entidad.getNumeroCarnet().isBlank()
                || entidad.getNumeroCarnet().equals("UMG-XXXXXXX")) {
            entidad.setNumeroCarnet(generarNumeroCarnetUnico());
        }

        if (foto != null && !foto.isEmpty()) {
            entidad.setFotoRuta(guardarFoto(foto));
            entidad.setEncodingFacial(null);
        } else if (fotoBase64 != null && !fotoBase64.isBlank()) {
            entidad.setFotoRuta(guardarFotoBase64(fotoBase64));
            entidad.setEncodingFacial(null);
        }

        if (persona.getContrasena() != null && !persona.getContrasena().isBlank()) {
            boolean yaEsBcrypt = persona.getContrasena().startsWith("$2a$")
                    || persona.getContrasena().startsWith("$2b$")
                    || persona.getContrasena().startsWith("$2y$");

            if (!yaEsBcrypt) {
                entidad.setContrasena(passwordEncoder.encode(persona.getContrasena()));
            } else {
                entidad.setContrasena(persona.getContrasena());
            }
        }

        if (entidad.getActivo() == null) {
            entidad.setActivo(true);
        }

        if (entidad.getRestringido() == null) {
            entidad.setRestringido(false);
        }

        Persona guardada = personaRepository.save(entidad);

        enrolarFacialmenteSiCorresponde(guardada);

        return guardada;
    }

    private void enrolarFacialmenteSiCorresponde(Persona guardada) {
        if (guardada.getFotoRuta() == null || guardada.getEncodingFacial() != null) {
            return;
        }

        try {
            Path rutaFoto = Paths.get(guardada.getFotoRuta()).isAbsolute()
                    ? Paths.get(guardada.getFotoRuta())
                    : Paths.get("").toAbsolutePath().resolve(guardada.getFotoRuta());

            log.info("Intentando enrolar foto: {}", rutaFoto);
            log.info("Existe el archivo: {}", Files.exists(rutaFoto));

            if (!Files.exists(rutaFoto)) {
                return;
            }

            byte[] fotoBytes = Files.readAllBytes(rutaFoto);
            String base64 = java.util.Base64.getEncoder().encodeToString(fotoBytes);

            List<Double> descriptor = facialApiService.enrolar(guardada.getId(), base64);

            if (descriptor != null) {
                guardada.setEncodingFacial(facialApiService.descriptorAJson(descriptor));
                personaRepository.save(guardada);
                log.info("Descriptor facial guardado para {}", guardada.getNombreCompleto());
            }

        } catch (Exception e) {
            log.warn("No se pudo enrolar facialmente: {}", e.getMessage(), e);
        }
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