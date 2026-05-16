package com.umg.biometrico.repository;

import com.umg.biometrico.model.Persona;
import com.umg.biometrico.model.RegistroIngreso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RegistroIngresoRepository extends JpaRepository<RegistroIngreso, Long> {

    List<RegistroIngreso> findByPuerta_IdAndFechaHoraBetweenOrderByFechaHoraAsc(
            Long puertaId, LocalDateTime inicio, LocalDateTime fin);

    List<RegistroIngreso> findByPuerta_IdAndFechaHoraBetweenOrderByFechaHoraDesc(
            Long puertaId, LocalDateTime inicio, LocalDateTime fin);

    List<RegistroIngreso> findTop100ByOrderByFechaHoraDesc();

    List<RegistroIngreso> findTop100ByPuerta_IdOrderByFechaHoraDesc(Long puertaId);

    @Query("SELECT DISTINCT CAST(r.fechaHora AS date) FROM RegistroIngreso r WHERE r.puerta.id = :puertaId ORDER BY CAST(r.fechaHora AS date) DESC")
    List<LocalDate> findFechasDistintasByPuerta(@Param("puertaId") Long puertaId);

    @Query("SELECT r FROM RegistroIngreso r WHERE r.puerta.instalacion.id = :instalacionId AND " +
           "r.fechaHora BETWEEN :inicio AND :fin ORDER BY r.fechaHora DESC")
    List<RegistroIngreso> findByInstalacionAndFecha(@Param("instalacionId") Long instalacionId,
                                                    @Param("inicio") LocalDateTime inicio,
                                                    @Param("fin") LocalDateTime fin);

    @Query("SELECT COUNT(r) FROM RegistroIngreso r WHERE r.fechaHora >= :inicio AND r.fechaHora < :fin")
    Long contarIngresosDia(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query("SELECT r FROM RegistroIngreso r WHERE r.puerta.id IN " +
           "(SELECT p.id FROM Puerta p WHERE p.instalacion.id = :instalacionId AND p.esSalon = true) " +
           "ORDER BY r.fechaHora DESC")
    List<RegistroIngreso> findIngresosASalonesByInstalacion(@Param("instalacionId") Long instalacionId);

    List<RegistroIngreso> findByPuerta_IdOrderByFechaHoraDesc(Long puertaId);

    @Query("SELECT DISTINCT r.persona FROM RegistroIngreso r " +
           "WHERE r.puerta.id = :puertaId AND r.persona IS NOT NULL")
    List<Persona> findPersonasDistintasByPuerta(@Param("puertaId") Long puertaId);

    @Query("SELECT r FROM RegistroIngreso r WHERE r.puerta.id = :puertaId " +
           "AND r.fechaHora BETWEEN :inicio AND :fin " +
           "AND r.persona.tipoPersona = 'CATEDRATICO' " +
           "ORDER BY r.fechaHora")
    List<RegistroIngreso> findCatedraticosEnSalonFecha(@Param("puertaId") Long puertaId,
                                                       @Param("inicio") LocalDateTime inicio,
                                                       @Param("fin") LocalDateTime fin);
}
