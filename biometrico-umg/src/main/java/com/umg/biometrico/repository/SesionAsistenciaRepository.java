package com.umg.biometrico.repository;

import com.umg.biometrico.model.SesionAsistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SesionAsistenciaRepository extends JpaRepository<SesionAsistencia, Long> {

    Optional<SesionAsistencia> findByCamara_IdAndActivaTrue(Long camaraId);

    Optional<SesionAsistencia> findByCurso_IdAndActivaTrue(Long cursoId);

    Optional<SesionAsistencia> findByPuerta_IdAndActivaTrue(Long puertaId);

    List<SesionAsistencia> findByCatedratico_IdAndActivaTrue(Long catedraticoId);

    List<SesionAsistencia> findByFechaAndActivaTrue(LocalDate fecha);

    List<SesionAsistencia> findByActivaTrue();

    boolean existsByCamara_IdAndActivaTrue(Long camaraId);

    boolean existsByCurso_IdAndActivaTrue(Long cursoId);
}