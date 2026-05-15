package com.umg.biometrico.repository;

import com.umg.biometrico.model.Camara;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CamaraRepository extends JpaRepository<Camara, Long> {

    List<Camara> findByActivaTrue();

    List<Camara> findByPuerta_IdAndActivaTrue(Long puertaId);

    Optional<Camara> findByIdAndActivaTrue(Long id);

    List<Camara> findByPuerta_Instalacion_Id(Long instalacionId);
}