package com.umg.biometrico.repository;

import com.umg.biometrico.model.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonaRepository extends JpaRepository<Persona, Long> {

    Optional<Persona> findByCorreo(String correo);

    Optional<Persona> findByNumeroCarnet(String numeroCarnet);

    List<Persona> findByTipoPersonaAndActivo(String tipoPersona, Boolean activo);

    List<Persona> findByActivoTrue();

    List<Persona> findByRestringidoTrue();

    @Query("SELECT p FROM Persona p WHERE p.activo = true AND " +
           "(LOWER(p.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(p.apellido) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(p.correo) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(p.numeroCarnet) LIKE LOWER(CONCAT('%', :busqueda, '%')))")
    List<Persona> buscarPersonas(@Param("busqueda") String busqueda);

    @Query("SELECT COUNT(p) FROM Persona p WHERE p.activo = true AND p.tipoPersona = :tipo")
    Long contarPorTipo(@Param("tipo") String tipo);

    @Query("SELECT COUNT(p) FROM Persona p WHERE p.activo = true")
    Long contarActivos();

    @Query("SELECT COUNT(p) FROM Persona p WHERE p.restringido = true")
    Long contarRestringidos();

    List<Persona> findByTipoPersonaIn(List<String> tipos);
}
