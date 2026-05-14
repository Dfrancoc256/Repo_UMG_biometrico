package com.umg.biometrico.repository;

import com.umg.biometrico.model.Curso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CursoRepository extends JpaRepository<Curso, Long> {

    List<Curso> findByCatedratico_IdAndActivoTrue(Long catedraticoId);

    long countByCatedratico_IdAndActivoTrue(Long catedraticoId);

    List<Curso> findByActivoTrue();
    
    List<Curso> findByActivoTrueOrderByCarreraNombreAscNombreAsc();

    @Query("SELECT c FROM Curso c JOIN c.estudiantes ce WHERE ce.estudiante.id = :estudianteId AND c.activo = true")
    List<Curso> findCursosByEstudiante(@Param("estudianteId") Long estudianteId);

    @Query("SELECT COUNT(c) FROM Curso c WHERE c.activo = true")
    Long contarActivos();
}
