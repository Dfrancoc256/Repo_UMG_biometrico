package com.umg.biometrico.repository;

import com.umg.biometrico.model.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonaRepository extends JpaRepository<Persona, Long> {

    Optional<Persona> findByCorreo(String correo);

    Optional<Persona> findByNumeroCarnet(String numeroCarnet);

    List<Persona> findByRol_NombreAndActivo(String nombreRol, Boolean activo);

    List<Persona> findByActivoTrue();

    List<Persona> findByRestringidoTrue();

    /** Restricción directa con UPDATE — evita problemas de caché Hibernate */
    @Transactional
    @Modifying
    @Query("UPDATE Persona p SET p.restringido = true, p.motivoRestriccion = :motivo WHERE p.id = :id")
    void restriccionDirecta(@Param("id") Long id, @Param("motivo") String motivo);

    /** Levanta restricción con UPDATE directo */
    @Transactional
    @Modifying
    @Query("UPDATE Persona p SET p.restringido = false, p.motivoRestriccion = null WHERE p.id = :id")
    void levantarRestriccionDirecta(@Param("id") Long id);

    /** Lista restringidos con JPQL explícito */
    @Query("SELECT p FROM Persona p WHERE p.restringido = true ORDER BY p.apellido, p.nombre")
    List<Persona> findRestringidosOrdenados();

    @Query("SELECT p FROM Persona p WHERE p.activo = true AND " +
           "(LOWER(p.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(p.apellido) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(p.correo) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(p.numeroCarnet) LIKE LOWER(CONCAT('%', :busqueda, '%')))")
    List<Persona> buscarPersonas(@Param("busqueda") String busqueda);

    @Query("SELECT COUNT(p) FROM Persona p WHERE p.activo = true AND p.rol.nombre = :tipo")
    Long contarPorRol(@Param("tipo") String tipo);

    @Query("SELECT COUNT(p) FROM Persona p WHERE p.activo = true")
    Long contarActivos();

    @Query("SELECT COUNT(p) FROM Persona p WHERE p.restringido = true")
    Long contarRestringidos();

    List<Persona> findByRol_NombreIn(List<String> nombresRol);

    List<Persona> findTop5ByActivoTrueOrderByIdDesc();
}
