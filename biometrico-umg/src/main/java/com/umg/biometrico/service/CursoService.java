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

import java.util.List;
import java.util.Optional;

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
        return cursoRepository.save(curso);
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

    public Long contarActivos() {
        return cursoRepository.contarActivos();
    }
}
