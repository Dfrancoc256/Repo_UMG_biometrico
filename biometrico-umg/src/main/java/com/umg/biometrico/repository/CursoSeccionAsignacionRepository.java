package com.umg.biometrico.repository;

import com.umg.biometrico.model.CursoSeccionAsignacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CursoSeccionAsignacionRepository extends JpaRepository<CursoSeccionAsignacion, Long> {

    List<CursoSeccionAsignacion> findByCurso_Id(Long cursoId);

    Optional<CursoSeccionAsignacion> findByCurso_IdAndSeccion(Long cursoId, String seccion);

    List<CursoSeccionAsignacion> findByCatedratico_Id(Long catedraticoId);

    boolean existsByCurso_IdAndSeccion(Long cursoId, String seccion);
}
