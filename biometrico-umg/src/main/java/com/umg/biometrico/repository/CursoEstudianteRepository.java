package com.umg.biometrico.repository;

import com.umg.biometrico.model.CursoEstudiante;
import com.umg.biometrico.model.CursoEstudianteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CursoEstudianteRepository extends JpaRepository<CursoEstudiante, CursoEstudianteId> {
    List<CursoEstudiante> findByCurso_Id(Long cursoId);
    Optional<CursoEstudiante> findByCurso_IdAndEstudiante_Id(Long cursoId, Long estudianteId);
    boolean existsByCurso_IdAndEstudiante_Id(Long cursoId, Long estudianteId);

    List<CursoEstudiante> findByEstudiante_Id(Long estudianteId);
}
