package com.kiert.backend.repository;

import com.kiert.backend.entity.Bloqueo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BloqueoRepository extends JpaRepository<Bloqueo, Long> {

    Optional<Bloqueo> findByUsuarioBloqueadorIdAndUsuarioBloqueadoIdAndActivoTrue(
            Long bloqueadorId, Long bloqueadoId);

    List<Bloqueo> findByUsuarioBloqueadorIdAndActivoTrue(Long usuarioId);

    List<Bloqueo> findByUsuarioBloqueadoIdAndActivoTrue(Long usuarioId);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN TRUE ELSE FALSE END FROM Bloqueo b " +
            "WHERE b.usuarioBloqueador.id = :bloqueadorId " +
            "AND b.usuarioBloqueado.id = :bloqueadoId " +
            "AND b.activo = true")
    boolean existeBloqueo(@Param("bloqueadorId") Long bloqueadorId, @Param("bloqueadoId") Long bloqueadoId);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN TRUE ELSE FALSE END FROM Bloqueo b " +
            "WHERE ((b.usuarioBloqueador.id = :usuario1 AND b.usuarioBloqueado.id = :usuario2) OR " +
            "(b.usuarioBloqueador.id = :usuario2 AND b.usuarioBloqueado.id = :usuario1)) " +
            "AND b.activo = true")
    boolean existeBloqueoEntreUsuarios(@Param("usuario1") Long usuario1, @Param("usuario2") Long usuario2);
}