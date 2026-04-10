package com.umg.biometrico.repository;

import com.umg.biometrico.model.Puerta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PuertaRepository extends JpaRepository<Puerta, Long> {
    List<Puerta> findByInstalacionId(Long instalacionId);
    List<Puerta> findByInstalacionIdAndEsSalon(Long instalacionId, Boolean esSalon);
}
