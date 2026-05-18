package com.umg.biometrico.service;

import com.umg.biometrico.model.Camara;
import com.umg.biometrico.model.Curso;
import com.umg.biometrico.model.Persona;
import com.umg.biometrico.model.Puerta;
import com.umg.biometrico.model.RegistroIngreso;
import com.umg.biometrico.model.SesionAsistencia;
import com.umg.biometrico.repository.CamaraRepository;
import com.umg.biometrico.repository.CursoRepository;
import com.umg.biometrico.repository.PersonaRepository;
import com.umg.biometrico.repository.PuertaRepository;
import com.umg.biometrico.repository.RegistroIngresoRepository;
import com.umg.biometrico.repository.SesionAsistenciaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

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
    private final CamaraRepository camaraRepository;
    private final SesionAsistenciaRepository sesionAsistenciaRepository;
    private final AsistenciaService asistenciaService;

    public RegistroIngreso registrarIngreso(Long personaId, Long puertaId, String metodo) {
        return registrarIngreso(personaId, puertaId, metodo, null);
    }

    public RegistroIngreso registrarIngreso(Long personaId, Long puertaId, String metodo, Long cursoId) {
        Persona persona = personaRepository.findById(personaId)
                .orElseThrow(() -> new RuntimeException("Persona no encontrada"));

        validarPersonaRestringida(persona);

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

    public RegistroIngreso registrarIngresoAsistenciaKiosko(
            Long personaId,
            Long puertaId,
            Long cursoId,
            Long camaraId,
            Long sesionId,
            Double similitud
    ) {
        Persona persona = personaRepository.findById(personaId)
                .orElseThrow(() -> new RuntimeException("Persona no encontrada"));

        validarPersonaRestringida(persona);

        Puerta puerta = puertaRepository.findById(puertaId)
                .orElseThrow(() -> new RuntimeException("Puerta no encontrada"));

        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new RuntimeException("Curso no encontrado"));

        Camara camara = camaraRepository.findById(camaraId)
                .orElseThrow(() -> new RuntimeException("Cámara no encontrada"));

        SesionAsistencia sesion = sesionAsistenciaRepository.findById(sesionId)
                .orElseThrow(() -> new RuntimeException("Sesión de asistencia no encontrada"));

        if (!Boolean.TRUE.equals(sesion.getActiva())) {
            throw new RuntimeException("La sesión de asistencia ya no está activa.");
        }

        RegistroIngreso registro = new RegistroIngreso();
        registro.setPersona(persona);
        registro.setPuerta(puerta);
        registro.setCurso(curso);
        registro.setCamara(camara);
        registro.setSesionAsistencia(sesion);
        registro.setFechaHora(LocalDateTime.now());

        registro.setMetodo("FACIAL");
        registro.setMetodoIngreso("KIOSKO");
        registro.setAccesoPermitido(true);
        registro.setSimilitudFacial(similitud);
        registro.setObservaciones("Ingreso registrado desde kiosko facial.");

        RegistroIngreso guardado = registroIngresoRepository.save(registro);

        asistenciaService.registrarAsistenciaIngreso(personaId, cursoId);

        return guardado;
    }

    private void validarPersonaRestringida(Persona persona) {
        if (Boolean.TRUE.equals(persona.getRestringido())) {
            String motivo = persona.getMotivoRestriccion() != null
                    ? persona.getMotivoRestriccion()
                    : "Sin motivo especificado";

            throw new RuntimeException(
                    "ACCESO DENEGADO: " + persona.getNombreCompleto()
                            + " tiene acceso restringido. Motivo: " + motivo
            );
        }
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

    public List<RegistroIngreso> obtenerTodosIngresosPorPuerta(Long puertaId) {
        return registroIngresoRepository.findByPuerta_IdOrderByFechaHoraDesc(puertaId);
    }

    public List<Persona> obtenerPersonasEnSalon(Long puertaId) {
        return registroIngresoRepository.findPersonasDistintasByPuerta(puertaId)
                .stream()
                .sorted(java.util.Comparator
                        .comparing((Persona p) -> p.getApellido() != null ? p.getApellido() : "")
                        .thenComparing(p -> p.getNombre() != null ? p.getNombre() : ""))
                .collect(java.util.stream.Collectors.toList());
    }

    public List<RegistroIngreso> obtenerCatedraticosEnSalonFecha(Long puertaId, LocalDate fecha) {
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(LocalTime.MAX);
        return registroIngresoRepository.findCatedraticosEnSalonFecha(puertaId, inicio, fin);
    }

    public List<Map<String, Object>> obtenerRecientes(int limit, Long puertaId) {
        List<RegistroIngreso> registros;

        if (puertaId != null) {
            registros = registroIngresoRepository
                    .findTop100ByPuerta_IdOrderByFechaHoraDesc(puertaId);
        } else {
            registros = registroIngresoRepository
                    .findTop100ByOrderByFechaHoraDesc();
        }

        return registros.stream()
                .limit(limit)
                .map(r -> {
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("nombre",    r.getPersona() != null ? r.getPersona().getNombreCompleto() : "—");
                    m.put("carnet",    r.getPersona() != null ? r.getPersona().getNumeroCarnet()   : "—");
                    m.put("puerta",    r.getPuerta()  != null ? r.getPuerta().getNombre()          : "—");
                    m.put("curso",     r.getCurso()   != null ? r.getCurso().getNombre()           : null);
                    m.put("confianza", r.getSimilitudFacial() != null
                            ? Math.round(r.getSimilitudFacial() * 100.0) / 100.0 : null);
                    m.put("fechaHora", r.getFechaHora() != null ? r.getFechaHora().toString() : null);
                    m.put("metodo",    r.getMetodo());
                    return m;
                })
                .collect(java.util.stream.Collectors.toList());
    }
}