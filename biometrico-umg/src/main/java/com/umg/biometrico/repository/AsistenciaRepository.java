package com.umg.biometrico.repository;

import com.umg.biometrico.model.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {

    List<Asistencia> findByCurso_IdAndFecha(Long cursoId, LocalDate fecha);

    Optional<Asistencia> findByEstudiante_IdAndCurso_IdAndFecha(Long estudianteId, Long cursoId, LocalDate fecha);

    List<Asistencia> findByEstudiante_IdAndCurso_Id(Long estudianteId, Long cursoId);

    @Query("SELECT COUNT(a) FROM Asistencia a WHERE a.presente = true AND a.fecha = :fecha")
    Long contarPresentesEnFecha(@Param("fecha") LocalDate fecha);

    @Query("SELECT a FROM Asistencia a WHERE a.curso.id = :cursoId AND a.fecha = :fecha ORDER BY a.estudiante.apellido")
    List<Asistencia> findByCursoAndFechaOrdenado(@Param("cursoId") Long cursoId, @Param("fecha") LocalDate fecha);

    @Query("SELECT COUNT(a) FROM Asistencia a WHERE a.curso.id = :cursoId AND a.fecha = :fecha AND a.presente = true")
    Long contarPresentesByCursoFecha(@Param("cursoId") Long cursoId, @Param("fecha") LocalDate fecha);

    @Query("SELECT a FROM Asistencia a LEFT JOIN FETCH a.curso LEFT JOIN FETCH a.estudiante " +
           "WHERE a.estudiante.id = :estudianteId ORDER BY a.fecha DESC, a.horaRegistro DESC")
    List<Asistencia> findByEstudianteIdConCurso(@Param("estudianteId") Long estudianteId);

    List<Asistencia> findByEstudiante_IdOrderByFechaDescHoraRegistroDesc(Long estudianteId);
    
}
