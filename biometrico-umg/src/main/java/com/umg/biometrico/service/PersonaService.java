package com.umg.biometrico.service;

import com.umg.biometrico.model.Persona;
import com.umg.biometrico.repository.PersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PersonaService {

    private final PersonaRepository personaRepository;

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

    public List<Persona> buscar(String termino) {
        return personaRepository.buscarPersonas(termino);
    }

    public List<Persona> listarEstudiantes() {
        return personaRepository.findByTipoPersonaAndActivo("estudiante", true);
    }

    public List<Persona> listarCatedraticos() {
        return personaRepository.findByTipoPersonaAndActivo("catedratico", true);
    }

    public Persona guardar(Persona persona, MultipartFile foto) throws IOException {
        if (persona.getNumeroCarnet() == null || persona.getNumeroCarnet().isBlank()) {
            persona.setNumeroCarnet(generarNumeroCarnet());
        }

        if (persona.getActivo() == null) {
            persona.setActivo(true);
        }

        if (persona.getRestringido() == null) {
            persona.setRestringido(false);
        }

        if (foto != null && !foto.isEmpty()) {
            String rutaFoto = guardarFoto(foto);
            persona.setFotoRuta(rutaFoto);
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

    private String generarNumeroCarnet() {
        return "UMG-" + String.valueOf(System.currentTimeMillis()).substring(7);
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

    public Long contarPorTipo(String tipo) {
        return personaRepository.contarPorTipo(tipo);
    }

    public Long contarActivos() {
        return personaRepository.contarActivos();
    }

    public Long contarRestringidos() {
        return personaRepository.contarRestringidos();
    }
}