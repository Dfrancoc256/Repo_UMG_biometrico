package com.umg.biometrico.repository;

import com.umg.biometrico.model.Instalacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstalacionRepository extends JpaRepository<Instalacion, Long> {

    @Query("SELECT DISTINCT i FROM Instalacion i LEFT JOIN FETCH i.puertas")
    List<Instalacion> findAllWithPuertas();
}
