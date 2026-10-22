// src/main/java/com/kiert/backend/repository/BloqueoRepository.java
package com.kiert.backend.features.bloqueo.repository;

import com.kiert.backend.entity.Bloqueo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface BloqueoRepository extends JpaRepository<Bloqueo, Long> {

    @Query("""
            SELECT b FROM Bloqueo b
            LEFT JOIN FETCH b.usuarioBloqueador
            LEFT JOIN FETCH b.usuarioBloqueado
            WHERE b.usuarioBloqueador.id = :bloqueadorId
              AND b.usuarioBloqueado.id = :bloqueadoId
              AND b.activo = true
            """)
    Optional<Bloqueo> findBloqueoActivo(
            @Param("bloqueadorId") Long bloqueadorId,
            @Param("bloqueadoId") Long bloqueadoId
    );

    @Query("""
            SELECT b FROM Bloqueo b
            LEFT JOIN FETCH b.usuarioBloqueador
            LEFT JOIN FETCH b.usuarioBloqueado
            WHERE b.activo = true
              AND (
                (b.usuarioBloqueador.id = :usuarioA AND b.usuarioBloqueado.id = :usuarioB)
                OR
                (b.usuarioBloqueador.id = :usuarioB AND b.usuarioBloqueado.id = :usuarioA)
              )
            """)
    Optional<Bloqueo> findBloqueoActivoEntre(
            @Param("usuarioA") Long usuarioA,
            @Param("usuarioB") Long usuarioB
    );

    @Query("""
            SELECT COUNT(b) > 0 FROM Bloqueo b
            WHERE b.usuarioBloqueador.id = :bloqueadorId
              AND b.usuarioBloqueado.id = :bloqueadoId
              AND b.activo = true
            """)
    boolean existeBloqueoActivo(
            @Param("bloqueadorId") Long bloqueadorId,
            @Param("bloqueadoId") Long bloqueadoId
    );

    @Query("""
            SELECT COUNT(b) > 0 FROM Bloqueo b
            WHERE b.activo = true
              AND (
                (b.usuarioBloqueador.id = :usuarioA AND b.usuarioBloqueado.id = :usuarioB)
                OR
                (b.usuarioBloqueador.id = :usuarioB AND b.usuarioBloqueado.id = :usuarioA)
              )
            """)
    boolean existeBloqueoEntre(
            @Param("usuarioA") Long usuarioA,
            @Param("usuarioB") Long usuarioB
    );

    @Query("""
            SELECT b FROM Bloqueo b
            LEFT JOIN FETCH b.usuarioBloqueado
            WHERE b.usuarioBloqueador.id = :bloqueadorId
              AND b.activo = true
            ORDER BY b.fechaCreacion DESC
            """)
    List<Bloqueo> findBloqueosActivosDeUsuario(@Param("bloqueadorId") Long bloqueadorId);

    @Query("""
            SELECT b FROM Bloqueo b
            LEFT JOIN FETCH b.usuarioBloqueado
            WHERE b.usuarioBloqueador.id = :bloqueadorId
              AND b.activo = true
            ORDER BY b.fechaCreacion DESC
            """)
    Page<Bloqueo> findBloqueosActivosDeUsuarioPaginado(
            @Param("bloqueadorId") Long bloqueadorId,
            Pageable pageable
    );

    @Query("""
            SELECT b FROM Bloqueo b
            LEFT JOIN FETCH b.usuarioBloqueador
            WHERE b.usuarioBloqueado.id = :bloqueadoId
              AND b.activo = true
            ORDER BY b.fechaCreacion DESC
            """)
    List<Bloqueo> findBloqueosRecibidos(@Param("bloqueadoId") Long bloqueadoId);

    @Query("""
            SELECT b FROM Bloqueo b
            LEFT JOIN FETCH b.usuarioBloqueador
            WHERE b.usuarioBloqueado.id = :bloqueadoId
              AND b.activo = true
            ORDER BY b.fechaCreacion DESC
            """)
    Page<Bloqueo> findBloqueosRecibidosPaginado(
            @Param("bloqueadoId") Long bloqueadoId,
            Pageable pageable
    );

    @Query("""
            SELECT b.usuarioBloqueado.id FROM Bloqueo b
            WHERE b.usuarioBloqueador.id = :usuarioId
              AND b.activo = true
            """)
    List<Long> findUsuariosBloqueadosIds(@Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT b.usuarioBloqueador.id FROM Bloqueo b
            WHERE b.usuarioBloqueado.id = :usuarioId
              AND b.activo = true
            """)
    List<Long> findUsuariosQueMeBloquearonIds(@Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT CASE
                WHEN b.usuarioBloqueador.id = :usuarioId THEN b.usuarioBloqueado.id
                ELSE b.usuarioBloqueador.id
            END
            FROM Bloqueo b
            WHERE b.activo = true
              AND (b.usuarioBloqueador.id = :usuarioId OR b.usuarioBloqueado.id = :usuarioId)
            """)
    List<Long> findUsuariosBloqueadosBidireccional(@Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT COUNT(b) FROM Bloqueo b
            WHERE b.usuarioBloqueador.id = :usuarioId
              AND b.activo = true
            """)
    long contarBloqueados(@Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT COUNT(b) FROM Bloqueo b
            WHERE b.usuarioBloqueado.id = :usuarioId
              AND b.activo = true
            """)
    long contarBloqueadores(@Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT b.usuarioBloqueado.id, COUNT(b) as total
            FROM Bloqueo b
            WHERE b.activo = true
            GROUP BY b.usuarioBloqueado.id
            ORDER BY total DESC
            """)
    List<Object[]> topUsuariosMasBloqueados(Pageable pageable);

    @Query("""
            SELECT b.usuarioBloqueador.id, COUNT(b) as total
            FROM Bloqueo b
            WHERE b.activo = true
            GROUP BY b.usuarioBloqueador.id
            ORDER BY total DESC
            """)
    List<Object[]> topUsuariosQueMasBloquean(Pageable pageable);

    @Query("""
            SELECT b FROM Bloqueo b
            LEFT JOIN FETCH b.usuarioBloqueado
            WHERE b.usuarioBloqueador.id = :usuarioId
            ORDER BY b.fechaCreacion DESC
            """)
    Page<Bloqueo> findHistorialDeUsuario(
            @Param("usuarioId") Long usuarioId,
            Pageable pageable
    );

    @Modifying
    @Query("""
            UPDATE Bloqueo b
            SET b.activo = false
            WHERE b.usuarioBloqueador.id = :bloqueadorId
              AND b.usuarioBloqueado.id = :bloqueadoId
              AND b.activo = true
            """)
    int desactivarBloqueo(
            @Param("bloqueadorId") Long bloqueadorId,
            @Param("bloqueadoId") Long bloqueadoId
    );

    @Modifying
    @Query("""
            UPDATE Bloqueo b
            SET b.activo = false
            WHERE b.usuarioBloqueador.id = :usuarioId
            """)
    int desactivarTodosDeUsuario(@Param("usuarioId") Long usuarioId);

    @Modifying
    @Query("""
            UPDATE Bloqueo b
            SET b.activo = false
            WHERE b.usuarioBloqueado.id = :usuarioId
            """)
    int desactivarTodosRecibidos(@Param("usuarioId") Long usuarioId);

    @Modifying
    @Query("""
            DELETE FROM Bloqueo b
            WHERE (b.usuarioBloqueador.id = :usuarioA AND b.usuarioBloqueado.id = :usuarioB)
               OR (b.usuarioBloqueador.id = :usuarioB AND b.usuarioBloqueado.id = :usuarioA)
            """)
    int eliminarEntreUsuarios(
            @Param("usuarioA") Long usuarioA,
            @Param("usuarioB") Long usuarioB
    );

    @Query("SELECT COUNT(b) FROM Bloqueo b WHERE b.activo = true")
    long countActivos();

    @Query("SELECT COUNT(b) FROM Bloqueo b")
    long countTotal();

    @Modifying
    @Query("""
            DELETE FROM Bloqueo b
            WHERE b.activo = false
              AND b.fechaCreacion < :limite
            """)
    int eliminarInactivosAntiguos(@Param("limite") Instant limite);
}