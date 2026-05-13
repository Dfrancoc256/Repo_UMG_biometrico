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

    public SesionAsistencia habilitarSesion(Long cursoId,
                                            Long catedraticoId,
                                            Long puertaId,
                                            Long camaraId) {

        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new RuntimeException("Curso no encontrado"));

        Persona catedratico = personaRepository.findById(catedraticoId)
                .orElseThrow(() -> new RuntimeException("Catedrático no encontrado"));

        Puerta puerta = puertaRepository.findById(puertaId)
                .orElseThrow(() -> new RuntimeException("Puerta o salón no encontrado"));

        Camara camara = camaraRepository.findById(camaraId)
                .orElseThrow(() -> new RuntimeException("Cámara no encontrada"));

        if (sesionRepository.existsByCamara_IdAndActivaTrue(camaraId)) {
            throw new RuntimeException("Esta cámara ya tiene una sesión activa.");
        }

        if (sesionRepository.existsByCurso_IdAndActivaTrue(cursoId)) {
            throw new RuntimeException("Este curso ya tiene una sesión activa.");
        }

        if (curso.getCatedratico() == null ||
                !curso.getCatedratico().getId().equals(catedraticoId)) {
            throw new RuntimeException("Este curso no pertenece al catedrático seleccionado.");
        }

        SesionAsistencia sesion = new SesionAsistencia();
        sesion.setCurso(curso);
        sesion.setCatedratico(catedratico);
        sesion.setPuerta(puerta);
        sesion.setCamara(camara);
        sesion.setFecha(LocalDate.now());
        sesion.setHoraInicio(LocalDateTime.now());
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
        return sesionRepository.findByCamara_IdAndActivaTrue(camaraId)
                .orElseThrow(() -> new RuntimeException("No hay sesión activa para esta cámara."));
    }

    public List<SesionAsistencia> listarSesionesActivasPorCatedratico(Long catedraticoId) {
        return sesionRepository.findByCatedratico_IdAndActivaTrue(catedraticoId);
    }

    public List<SesionAsistencia> listarSesionesActivasHoy() {
        return sesionRepository.findByFechaAndActivaTrue(LocalDate.now());
    }
}