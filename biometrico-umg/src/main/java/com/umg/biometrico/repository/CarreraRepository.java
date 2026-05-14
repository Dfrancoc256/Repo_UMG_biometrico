package com.umg.biometrico.repository;

import com.umg.biometrico.model.Carrera;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarreraRepository extends JpaRepository<Carrera, Long> {

    List<Carrera> findByActivoTrue();
}