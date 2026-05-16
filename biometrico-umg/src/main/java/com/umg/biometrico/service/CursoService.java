package com.umg.biometrico.service;

import com.umg.biometrico.model.Curso;
import com.umg.biometrico.model.CursoEstudiante;
import com.umg.biometrico.model.Persona;
import com.umg.biometrico.repository.CursoEstudianteRepository;
import com.umg.biometrico.repository.CursoRepository;
import com.umg.biometrico.repository.PersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CursoService {

    private final CursoRepository cursoRepository;
    private final CursoEstudianteRepository cursoEstudianteRepository;
    private final PersonaRepository personaRepository;

    public List<Curso> listarTodos() {
        return cursoRepository.findAll();
    }

    public List<Curso> listarActivos() {
        return cursoRepository.findByActivoTrue();
    }

    public Optional<Curso> buscarPorId(Long id) {
        return cursoRepository.findById(id);
    }

    public List<Curso> listarPorCatedratico(Long catedraticoId) {
        return cursoRepository.findByCatedratico_IdAndActivoTrue(catedraticoId);
    }

    public Curso guardar(Curso curso) {
        if (curso.getId() == null && curso.getCatedratico() != null) {
            long count = cursoRepository.countByCatedratico_IdAndActivoTrue(curso.getCatedratico().getId());
            if (count >= 5) {
                throw new IllegalArgumentException(
                        "El catedrático ya tiene 5 cursos asignados (límite máximo). " +
                        "Debe quitar un curso antes de asignar uno nuevo.");
            }
        }
        if (curso.getCodigo() == null || curso.getCodigo().isBlank()) {
            curso.setCodigo(generarCodigoCurso());
        }
        return cursoRepository.save(curso);
    }

    public long contarCursosPorCatedratico(Long catedraticoId) {
        return cursoRepository.countByCatedratico_IdAndActivoTrue(catedraticoId);
    }

    public String generarCodigoPreview() {
        return generarCodigoCurso();
    }

    private String generarCodigoCurso() {
        int anio = Year.now().getValue();
        long seq = cursoRepository.countByCodigoAnio(String.valueOf(anio)) + 1;

        String codigo;

        do {
            codigo = String.format("CUR-%d-%03d", anio, seq);
            seq++;
        } while (cursoRepository.existsByCodigo(codigo));

        return codigo;
    }

    public void inscribirEstudiante(Long cursoId, Long estudianteId) {
        if (!cursoEstudianteRepository.existsByCurso_IdAndEstudiante_Id(cursoId, estudianteId)) {
            CursoEstudiante ce = new CursoEstudiante();
            Curso curso = cursoRepository.findById(cursoId).orElseThrow();
            Persona estudiante = personaRepository.findById(estudianteId).orElseThrow();
            ce.setCurso(curso);
            ce.setEstudiante(estudiante);
            cursoEstudianteRepository.save(ce);
        }
    }

    public void desinscribirEstudiante(Long cursoId, Long estudianteId) {
        cursoEstudianteRepository.findByCurso_IdAndEstudiante_Id(cursoId, estudianteId)
                .ifPresent(cursoEstudianteRepository::delete);
    }

    public List<CursoEstudiante> listarEstudiantesDeCurso(Long cursoId) {
        return cursoEstudianteRepository.findByCurso_Id(cursoId);
    }

    public List<Curso> obtenerCursosDeEstudiante(Long estudianteId) {
        return cursoRepository.findCursosByEstudiante(estudianteId);
    }

    public Long contarActivos() {
        return cursoRepository.contarActivos();
    }

    public Map<String, List<Curso>> listarActivosAgrupadosPorCarrera() {
        return cursoRepository.findByActivoTrue().stream()
                .collect(Collectors.groupingBy(
                        c -> c.getCarrera() != null && c.getCarrera().getNombre() != null && !c.getCarrera().getNombre().isBlank()
                                ? c.getCarrera().getNombre()
                                : "Sin facultad asignada",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }
}