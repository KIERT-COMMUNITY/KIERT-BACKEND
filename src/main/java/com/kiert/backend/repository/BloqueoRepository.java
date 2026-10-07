// src/main/java/com/kiert/backend/repository/BloqueoRepository.java
package com.kiert.backend.repository;

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

    // ============================================================
    // BUSCAR BLOQUEO ESPECÍFICO
    // ============================================================

    /**
     * Busca el bloqueo activo entre 2 usuarios (A bloquea a B).
     * Ya tenía JOIN FETCH, lo mantengo.
     */
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

    /**
     * NUEVO: Busca un bloqueo en CUALQUIER dirección (A→B o B→A).
     * Útil para verificar si hay bloqueo entre dos usuarios.
     */
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

    // ============================================================
    // VERIFICAR BLOQUEOS
    // ============================================================

    /**
     * Verifica si A bloqueó a B (direccional).
     */
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

    /**
     * Verifica si hay bloqueo entre A y B en CUALQUIER dirección.
     */
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

    // ============================================================
    // LISTAR BLOQUEOS DEL USUARIO
    // ============================================================

    /**
     * Lista bloqueos hechos por un usuario (a quién bloqueó).
     * Sin paginación. Usa la versión paginada para usuarios con muchos.
     */
    @Query("""
            SELECT b FROM Bloqueo b
            LEFT JOIN FETCH b.usuarioBloqueado
            WHERE b.usuarioBloqueador.id = :bloqueadorId
              AND b.activo = true
            ORDER BY b.fechaCreacion DESC
            """)
    List<Bloqueo> findBloqueosActivosDeUsuario(@Param("bloqueadorId") Long bloqueadorId);

    /**
     * NUEVO: Versión paginada.
     */
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

    /**
     * Lista bloqueos recibidos por un usuario (quién me bloqueó).
     */
    @Query("""
            SELECT b FROM Bloqueo b
            LEFT JOIN FETCH b.usuarioBloqueador
            WHERE b.usuarioBloqueado.id = :bloqueadoId
              AND b.activo = true
            ORDER BY b.fechaCreacion DESC
            """)
    List<Bloqueo> findBloqueosRecibidos(@Param("bloqueadoId") Long bloqueadoId);

    /**
     * NUEVO: Versión paginada.
     */
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

    // ============================================================
    // IDs PARA FILTRADO (chat, búsqueda, etc.)
    // ============================================================

    /**
     * IDs de usuarios que YO bloqueé.
     * Se usa en ChatService para filtrar conversaciones.
     */
    @Query("""
            SELECT b.usuarioBloqueado.id FROM Bloqueo b
            WHERE b.usuarioBloqueador.id = :usuarioId
              AND b.activo = true
            """)
    List<Long> findUsuariosBloqueadosIds(@Param("usuarioId") Long usuarioId);

    /**
     * IDs de usuarios que ME bloquearon.
     */
    @Query("""
            SELECT b.usuarioBloqueador.id FROM Bloqueo b
            WHERE b.usuarioBloqueado.id = :usuarioId
              AND b.activo = true
            """)
    List<Long> findUsuariosQueMeBloquearonIds(@Param("usuarioId") Long usuarioId);

    /**
     * NUEVO: IDs de usuarios bloqueados en CUALQUIER dirección.
     * Útil para "usuarios disponibles" (excluir bloqueados bidireccional).
     */
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

    // ============================================================
    // CONTADORES (para badges / perfil)
    // ============================================================

    /**
     * NUEVO: Cuenta cuántos usuarios he bloqueado.
     */
    @Query("""
            SELECT COUNT(b) FROM Bloqueo b
            WHERE b.usuarioBloqueador.id = :usuarioId
              AND b.activo = true
            """)
    long contarBloqueados(@Param("usuarioId") Long usuarioId);

    /**
     * NUEVO: Cuenta cuántos usuarios me bloquearon.
     */
    @Query("""
            SELECT COUNT(b) FROM Bloqueo b
            WHERE b.usuarioBloqueado.id = :usuarioId
              AND b.activo = true
            """)
    long contarBloqueadores(@Param("usuarioId") Long usuarioId);

    // ============================================================
    // TOP USUARIOS BLOQUEADOS (para detectar patrones)
    // ============================================================

    /**
     * NUEVO: Top usuarios más bloqueados (no activos, históricos).
     * Devuelve [usuarioId, count].
     */
    @Query("""
            SELECT b.usuarioBloqueado.id, COUNT(b) as total
            FROM Bloqueo b
            WHERE b.activo = true
            GROUP BY b.usuarioBloqueado.id
            ORDER BY total DESC
            """)
    List<Object[]> topUsuariosMasBloqueados(Pageable pageable);

    /**
     * NUEVO: Top usuarios que más bloquean.
     */
    @Query("""
            SELECT b.usuarioBloqueador.id, COUNT(b) as total
            FROM Bloqueo b
            WHERE b.activo = true
            GROUP BY b.usuarioBloqueador.id
            ORDER BY total DESC
            """)
    List<Object[]> topUsuariosQueMasBloquean(Pageable pageable);

    // ============================================================
    // HISTORIAL (incluyendo inactivos)
    // ============================================================

    /**
     * NUEVO: Historial completo de bloqueos de un usuario (activos e inactivos).
     */
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

    // ============================================================
    // DESBLOQUEAR / DESACTIVAR (bulk)
    // ============================================================

    /**
     * NUEVO: Desactiva un bloqueo específico (desbloquear).
     */
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

    /**
     * NUEVO: Desactiva TODOS los bloqueos de un usuario (hechos por él).
     * Útil al eliminar cuenta.
     */
    @Modifying
    @Query("""
            UPDATE Bloqueo b
            SET b.activo = false
            WHERE b.usuarioBloqueador.id = :usuarioId
            """)
    int desactivarTodosDeUsuario(@Param("usuarioId") Long usuarioId);

    /**
     * NUEVO: Desactiva TODOS los bloqueos RECIBIDOS por un usuario.
     */
    @Modifying
    @Query("""
            UPDATE Bloqueo b
            SET b.activo = false
            WHERE b.usuarioBloqueado.id = :usuarioId
            """)
    int desactivarTodosRecibidos(@Param("usuarioId") Long usuarioId);

    // ============================================================
    // ELIMINAR
    // ============================================================

    /**
     * NUEVO: Elimina (hard delete) TODOS los bloqueos entre dos usuarios.
     * Al eliminar contacto, se eliminan los bloqueos.
     */
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

    // ============================================================
    // ESTADÍSTICAS
    // ============================================================

    /**
     * NUEVO: Total de bloqueos activos en el sistema.
     */
    @Query("SELECT COUNT(b) FROM Bloqueo b WHERE b.activo = true")
    long countActivos();

    /**
     * NUEVO: Total de bloqueos históricos (incluyendo inactivos).
     */
    @Query("SELECT COUNT(b) FROM Bloqueo b")
    long countTotal();

    // ============================================================
    // LIMPIEZA (para @Scheduled)
    // ============================================================

    /**
     * NUEVO: Elimina (hard delete) bloqueos inactivos con más de N días.
     * Se ejecuta con un @Scheduled para mantener la tabla pequeña.
     */
    @Modifying
    @Query("""
            DELETE FROM Bloqueo b
            WHERE b.activo = false
              AND b.fechaCreacion < :limite
            """)
    int eliminarInactivosAntiguos(@Param("limite") Instant limite);
}