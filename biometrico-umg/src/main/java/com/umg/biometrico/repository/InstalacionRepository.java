package com.umg.biometrico.repository;

import com.umg.biometrico.model.Instalacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstalacionRepository extends JpaRepository<Instalacion, Long> {

    @Query("SELECT DISTINCT i FROM Instalacion i LEFT JOIN FETCH i.puertas")
    List<Instalacion> findAllWithPuertas();

    @Query("SELECT i FROM Instalacion i LEFT JOIN FETCH i.puertas WHERE i.id = :id")
    Optional<Instalacion> findByIdWithPuertas(@Param("id") Long id);
}
