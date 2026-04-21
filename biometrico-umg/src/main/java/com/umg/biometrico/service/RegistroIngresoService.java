package com.umg.biometrico.service;

import com.umg.biometrico.model.Curso;
import com.umg.biometrico.model.Persona;
import com.umg.biometrico.model.Puerta;
import com.umg.biometrico.model.RegistroIngreso;
import com.umg.biometrico.repository.CursoRepository;
import com.umg.biometrico.repository.PersonaRepository;
import com.umg.biometrico.repository.PuertaRepository;
import com.umg.biometrico.repository.RegistroIngresoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RegistroIngresoService {

    private final RegistroIngresoRepository registroIngresoRepository;
    private final PersonaRepository personaRepository;
    private final PuertaRepository puertaRepository;
    private final CursoRepository cursoRepository;
    private final AsistenciaService asistenciaService;

    // ─── Original: sin curso ──────────────────────────────────────────────────
    public RegistroIngreso registrarIngreso(Long personaId, Long puertaId, String metodo) {
        return registrarIngreso(personaId, puertaId, metodo, null);
    }

    // ─── Con curso opcional ───────────────────────────────────────────────────
    public RegistroIngreso registrarIngreso(Long personaId, Long puertaId, String metodo, Long cursoId) {
        Persona persona = personaRepository.findById(personaId)
                .orElseThrow(() -> new RuntimeException("Persona no encontrada"));

        // ─── Verificar restricción ────────────────────────────────────────────
        if (Boolean.TRUE.equals(persona.getRestringido())) {
            String motivo = persona.getMotivoRestriccion() != null
                    ? persona.getMotivoRestriccion()
                    : "Sin motivo especificado";
            throw new RuntimeException("ACCESO DENEGADO: " + persona.getNombreCompleto() +
                    " tiene acceso restringido. Motivo: " + motivo);
        }

        Puerta puerta = puertaRepository.findById(puertaId)
                .orElseThrow(() -> new RuntimeException("Puerta no encontrada"));

        RegistroIngreso registro = new RegistroIngreso();
        registro.setPersona(persona);
        registro.setPuerta(puerta);
        registro.setFechaHora(LocalDateTime.now());
        registro.setMetodo(metodo);

        if (cursoId != null) {
            cursoRepository.findById(cursoId).ifPresent(registro::setCurso);
        }

        return registroIngresoRepository.save(registro);
    }

    public List<RegistroIngreso> obtenerIngresosPorPuertaYFecha(Long puertaId, LocalDate fecha, String orden) {
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(LocalTime.MAX);
        if ("asc".equalsIgnoreCase(orden)) {
            return registroIngresoRepository
                    .findByPuerta_IdAndFechaHoraBetweenOrderByFechaHoraAsc(puertaId, inicio, fin);
        }
        return registroIngresoRepository
                .findByPuerta_IdAndFechaHoraBetweenOrderByFechaHoraDesc(puertaId, inicio, fin);
    }

    public List<LocalDate> obtenerFechasConIngreso(Long puertaId) {
        return registroIngresoRepository.findFechasDistintasByPuerta(puertaId);
    }

    public Long contarIngresosHoy() {
        LocalDateTime inicio = LocalDate.now().atStartOfDay();
        LocalDateTime fin = LocalDate.now().atTime(LocalTime.MAX);
        return registroIngresoRepository.contarIngresosDia(inicio, fin);
    }

    public List<RegistroIngreso> obtenerIngresosPorInstalacionYFecha(Long instalacionId, LocalDate fecha) {
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(LocalTime.MAX);
        return registroIngresoRepository.findByInstalacionAndFecha(instalacionId, inicio, fin);
    }

    public List<RegistroIngreso> obtenerIngresosASalones(Long instalacionId) {
        return registroIngresoRepository.findIngresosASalonesByInstalacion(instalacionId);
    }
}