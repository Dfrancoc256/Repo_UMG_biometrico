package com.umg.biometrico.service;

import com.umg.biometrico.model.*;
import com.umg.biometrico.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SesionAsistenciaService {

    private final SesionAsistenciaRepository sesionRepository;
    private final CursoRepository cursoRepository;
    private final PersonaRepository personaRepository;
    private final PuertaRepository puertaRepository;
    private final CamaraRepository camaraRepository;

    private static final int HORAS_EXPIRACION = 3;

    public SesionAsistencia habilitarSesion(Long cursoId,
                                            Long usuarioId,
                                            Long puertaId,
                                            Long camaraId) {

        cerrarSesionesExpiradas();

        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new RuntimeException("Curso no encontrado"));

        Persona usuario = personaRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Puerta puerta = puertaRepository.findById(puertaId)
                .orElseThrow(() -> new RuntimeException("Puerta o salón no encontrado"));

        Camara camara = camaraRepository.findById(camaraId)
                .orElseThrow(() -> new RuntimeException("Cámara no encontrada"));

        boolean esAdmin = usuario.getRol() != null
                && usuario.getRol().getNombre() != null
                && usuario.getRol().getNombre().equalsIgnoreCase("ADMIN");

        boolean esCatedratico = usuario.getRol() != null
                && usuario.getRol().getNombre() != null
                && usuario.getRol().getNombre().equalsIgnoreCase("CATEDRATICO");

        if (esCatedratico) {
            if (curso.getCatedratico() == null ||
                    !curso.getCatedratico().getId().equals(usuarioId)) {
                throw new RuntimeException("Este curso no pertenece al catedrático seleccionado.");
            }
        }

        if (!esAdmin && !esCatedratico) {
            throw new RuntimeException("No tiene permisos para iniciar asistencia.");
        }

        LocalDateTime ahora = LocalDateTime.now();

        var sesionCursoActiva = sesionRepository.findByCurso_IdAndActivaTrue(cursoId);

        if (sesionCursoActiva.isPresent()) {
            SesionAsistencia existente = sesionCursoActiva.get();

            if (existente.getCamara() != null && existente.getCamara().getId().equals(camaraId)) {
                return existente;
            }

            existente.setActiva(false);
            existente.setHoraFin(ahora);
            sesionRepository.save(existente);
        }

        var sesionCamaraActiva = sesionRepository.findByCamara_IdAndActivaTrue(camaraId);

        if (sesionCamaraActiva.isPresent()) {
            SesionAsistencia existente = sesionCamaraActiva.get();
            existente.setActiva(false);
            existente.setHoraFin(ahora);
            sesionRepository.save(existente);
        }

        SesionAsistencia sesion = new SesionAsistencia();
        sesion.setCurso(curso);
        sesion.setCatedratico(esCatedratico ? usuario : curso.getCatedratico());
        sesion.setPuerta(puerta);
        sesion.setCamara(camara);
        sesion.setFecha(LocalDate.now());
        sesion.setHoraInicio(ahora);
        sesion.setHoraFin(null);
        sesion.setExpiraEn(ahora.plusHours(HORAS_EXPIRACION));
        sesion.setActiva(true);

        return sesionRepository.save(sesion);
    }

    public SesionAsistencia finalizarSesion(Long sesionId) {
        SesionAsistencia sesion = sesionRepository.findById(sesionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        sesion.setActiva(false);
        sesion.setHoraFin(LocalDateTime.now());

        return sesionRepository.save(sesion);
    }

    public SesionAsistencia obtenerSesionActivaPorCamara(Long camaraId) {
        cerrarSesionesExpiradas();

        return sesionRepository.findByCamara_IdAndActivaTrue(camaraId)
                .orElseThrow(() -> new RuntimeException("No hay sesión activa para esta cámara."));
    }

    public List<SesionAsistencia> listarSesionesActivasPorCatedratico(Long catedraticoId) {
        cerrarSesionesExpiradas();
        return sesionRepository.findByCatedratico_IdAndActivaTrue(catedraticoId);
    }

    public List<SesionAsistencia> listarSesionesActivasHoy() {
        cerrarSesionesExpiradas();
        return sesionRepository.findByFechaAndActivaTrue(LocalDate.now());
    }

    public void cerrarSesionesExpiradas() {
        List<SesionAsistencia> sesionesActivas = sesionRepository.findByActivaTrue();
        LocalDateTime ahora = LocalDateTime.now();

        for (SesionAsistencia sesion : sesionesActivas) {
            boolean expiradaPorHora = sesion.getExpiraEn() != null && sesion.getExpiraEn().isBefore(ahora);
            boolean expiradaSinFecha = sesion.getExpiraEn() == null
                    && sesion.getHoraInicio() != null
                    && sesion.getHoraInicio().plusHours(HORAS_EXPIRACION).isBefore(ahora);

            if (expiradaPorHora || expiradaSinFecha) {
                sesion.setActiva(false);
                sesion.setHoraFin(ahora);
                sesionRepository.save(sesion);
            }
        }
    }
}