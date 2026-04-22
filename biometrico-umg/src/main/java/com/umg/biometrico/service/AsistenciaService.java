package com.umg.biometrico.service;

import com.umg.biometrico.dto.AsistenciaDTO;
import com.umg.biometrico.model.Asistencia;
import com.umg.biometrico.model.Curso;
import com.umg.biometrico.model.CursoEstudiante;
import com.umg.biometrico.model.Persona;
import com.umg.biometrico.repository.AsistenciaRepository;
import com.umg.biometrico.repository.CursoEstudianteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AsistenciaService {

    private final AsistenciaRepository asistenciaRepository;
    private final CursoEstudianteRepository cursoEstudianteRepository;

    public List<AsistenciaDTO> obtenerArbolAsistencia(Long cursoId, LocalDate fecha) {
        List<CursoEstudiante> inscritos = cursoEstudianteRepository.findByCurso_Id(cursoId);
        List<AsistenciaDTO> resultado = new ArrayList<>();

        for (CursoEstudiante ce : inscritos) {
            Persona e = ce.getEstudiante();
            Optional<Asistencia> asistencia = asistenciaRepository
                    .findByEstudiante_IdAndCurso_IdAndFecha(e.getId(), cursoId, fecha);

            AsistenciaDTO dto = new AsistenciaDTO();
            dto.setEstudianteId(e.getId());
            dto.setNombreCompleto(e.getNombreCompleto());
            dto.setCorreo(e.getCorreo());
            dto.setFotoRuta(e.getFotoRuta());
            dto.setSeccion(e.getSeccion());
            dto.setPresente(asistencia.map(Asistencia::getPresente).orElse(false));

            if (asistencia.isPresent() && asistencia.get().getHoraRegistro() != null) {
                dto.setHoraRegistro(asistencia.get().getHoraRegistro()
                        .format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            }
            resultado.add(dto);
        }
        return resultado;
    }

    public void confirmarAsistencia(Long cursoId, LocalDate fecha, List<Long> presentesIds) {
        List<CursoEstudiante> inscritos = cursoEstudianteRepository.findByCurso_Id(cursoId);

        for (CursoEstudiante ce : inscritos) {
            Long estudianteId = ce.getEstudiante().getId();
            Optional<Asistencia> existente = asistenciaRepository
                    .findByEstudiante_IdAndCurso_IdAndFecha(estudianteId, cursoId, fecha);

            Asistencia asistencia = existente.orElseGet(() -> {
                Asistencia nueva = new Asistencia();
                nueva.setEstudiante(ce.getEstudiante());
                nueva.setCurso(ce.getCurso());
                nueva.setFecha(fecha);
                return nueva;
            });

            boolean presente = presentesIds != null && presentesIds.contains(estudianteId);
            asistencia.setPresente(presente);
            if (presente) {
                asistencia.setHoraRegistro(LocalDateTime.now());
            }
            asistenciaRepository.save(asistencia);
        }
    }

    public void registrarAsistenciaIngreso(Long personaId, Long cursoId) {
        Optional<CursoEstudiante> inscripcion = cursoEstudianteRepository
                .findByCurso_IdAndEstudiante_Id(cursoId, personaId);
        if (inscripcion.isEmpty()) return;

        LocalDate hoy = LocalDate.now();
        Asistencia asistencia = asistenciaRepository
                .findByEstudiante_IdAndCurso_IdAndFecha(personaId, cursoId, hoy)
                .orElseGet(() -> {
                    Asistencia nueva = new Asistencia();
                    nueva.setEstudiante(inscripcion.get().getEstudiante());
                    nueva.setCurso(inscripcion.get().getCurso());
                    nueva.setFecha(hoy);
                    return nueva;
                });

        asistencia.setPresente(true);
        asistencia.setHoraRegistro(LocalDateTime.now());
        asistenciaRepository.save(asistencia);
    }

    public Long contarAsistenciasHoy() {
        LocalDate hoy = LocalDate.now();
        return asistenciaRepository.contarPresentesEnFecha(hoy);
    }

    public List<Asistencia> obtenerHistorialEstudiante(Long estudianteId, Long cursoId) {
        return asistenciaRepository.findByEstudiante_IdAndCurso_Id(estudianteId, cursoId);
    }

    public List<Asistencia> obtenerAsistenciasPorCursoYFecha(Long cursoId, LocalDate fecha) {
        return asistenciaRepository.findByCursoAndFechaOrdenado(cursoId, fecha);
    }

    public List<Asistencia> obtenerAsistenciaDeEstudiante(Long estudianteId) {
        return asistenciaRepository.findByEstudiante_IdOrderByFechaDescHoraRegistroDesc(estudianteId);
    }

}
