package com.kiert.backend.repository;

import com.kiert.backend.entity.Bloqueo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BloqueoRepository extends JpaRepository<Bloqueo, Long> {

    @Query("SELECT b FROM Bloqueo b " +
            "LEFT JOIN FETCH b.usuarioBloqueador " +
            "LEFT JOIN FETCH b.usuarioBloqueado " +
            "WHERE b.usuarioBloqueador.id = :bloqueadorId " +
            "AND b.usuarioBloqueado.id = :bloqueadoId " +
            "AND b.activo = true")
    Optional<Bloqueo> findBloqueoActivo(
            @Param("bloqueadorId") Long bloqueadorId,
            @Param("bloqueadoId") Long bloqueadoId
    );

    @Query("SELECT COUNT(b) > 0 FROM Bloqueo b " +
            "WHERE b.usuarioBloqueador.id = :bloqueadorId " +
            "AND b.usuarioBloqueado.id = :bloqueadoId " +
            "AND b.activo = true")
    boolean existeBloqueoActivo(
            @Param("bloqueadorId") Long bloqueadorId,
            @Param("bloqueadoId") Long bloqueadoId
    );

    @Query("SELECT b FROM Bloqueo b " +
            "LEFT JOIN FETCH b.usuarioBloqueado " +
            "WHERE b.usuarioBloqueador.id = :bloqueadorId " +
            "AND b.activo = true " +
            "ORDER BY b.fechaCreacion DESC")
    List<Bloqueo> findBloqueosActivosDeUsuario(@Param("bloqueadorId") Long bloqueadorId);

    @Query("SELECT b FROM Bloqueo b " +
            "LEFT JOIN FETCH b.usuarioBloqueador " +
            "WHERE b.usuarioBloqueado.id = :bloqueadoId " +
            "AND b.activo = true " +
            "ORDER BY b.fechaCreacion DESC")
    List<Bloqueo> findBloqueosRecibidos(@Param("bloqueadoId") Long bloqueadoId);

    // ✅ Saber si A bloqueó a B o B bloqueó a A (para el chat)
    @Query("SELECT COUNT(b) > 0 FROM Bloqueo b " +
            "WHERE b.activo = true AND (" +
            "(b.usuarioBloqueador.id = :usuarioA AND b.usuarioBloqueado.id = :usuarioB) OR " +
            "(b.usuarioBloqueador.id = :usuarioB AND b.usuarioBloqueado.id = :usuarioA))")
    boolean existeBloqueoEntre(
            @Param("usuarioA") Long usuarioA,
            @Param("usuarioB") Long usuarioB
    );
}