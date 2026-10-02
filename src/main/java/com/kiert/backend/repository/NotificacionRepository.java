// src/main/java/com/kiert/backend/repository/NotificacionRepository.java
package com.kiert.backend.repository;

import com.kiert.backend.entity.Notificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    // ============================================================
    // LISTAR NOTIFICACIONES
    // ============================================================

    /**
     * Lista TODAS las notificaciones del usuario con relaciones cargadas.
     * ⚠️ Para usuarios con muchas notificaciones, usa `findByUsuarioPaginado`.
     */
    @Query("""
            SELECT DISTINCT n FROM Notificacion n
            LEFT JOIN FETCH n.usuarioOrigen
            LEFT JOIN FETCH n.post
            LEFT JOIN FETCH n.comentario
            LEFT JOIN FETCH n.respuesta
            LEFT JOIN FETCH n.grupo
            WHERE n.usuarioDestino.id = :usuarioId
            ORDER BY n.fechaCreacion DESC
            """)
    List<Notificacion> findByUsuarioDestinoIdOrderByFechaCreacionDesc(
            @Param("usuarioId") Long usuarioId
    );

    /**
     * ✅ NUEVO: Versión paginada (recomendada).
     */
    @Query("""
            SELECT DISTINCT n FROM Notificacion n
            LEFT JOIN FETCH n.usuarioOrigen
            LEFT JOIN FETCH n.post
            LEFT JOIN FETCH n.comentario
            LEFT JOIN FETCH n.respuesta
            LEFT JOIN FETCH n.grupo
            WHERE n.usuarioDestino.id = :usuarioId
            ORDER BY n.fechaCreacion DESC
            """)
    Page<Notificacion> findByUsuarioPaginado(
            @Param("usuarioId") Long usuarioId,
            Pageable pageable
    );

    /**
     * ✅ NUEVO: Últimas N notificaciones (para el dropdown del navbar).
     * Mucho más rápido que cargar todas.
     */
    @Query("""
            SELECT n FROM Notificacion n
            LEFT JOIN FETCH n.usuarioOrigen
            LEFT JOIN FETCH n.post
            LEFT JOIN FETCH n.grupo
            WHERE n.usuarioDestino.id = :usuarioId
            ORDER BY n.fechaCreacion DESC
            """)
    List<Notificacion> findRecientes(
            @Param("usuarioId") Long usuarioId,
            Pageable pageable
    );

    // ============================================================
    // NO LEÍDAS
    // ============================================================

    /**
     * Contar no leídas (para el badge del navbar).
     * ⚠️ Este método se llama en CADA navegación. Cachear en Redis.
     */
    @Query("""
            SELECT COUNT(n) FROM Notificacion n
            WHERE n.usuarioDestino.id = :usuarioId
              AND n.leida = false
            """)
    long countNoLeidasByUsuario(@Param("usuarioId") Long usuarioId);

    /**
     * Obtener solo no leídas (con JOIN FETCH).
     */
    @Query("""
            SELECT n FROM Notificacion n
            LEFT JOIN FETCH n.usuarioOrigen
            LEFT JOIN FETCH n.post
            LEFT JOIN FETCH n.comentario
            LEFT JOIN FETCH n.respuesta
            LEFT JOIN FETCH n.grupo
            WHERE n.usuarioDestino.id = :usuarioId
              AND n.leida = false
            ORDER BY n.fechaCreacion DESC
            """)
    List<Notificacion> findNoLeidasByUsuario(@Param("usuarioId") Long usuarioId);

    /**
     * ✅ NUEVO: Últimas N no leídas (para el dropdown).
     */
    @Query("""
            SELECT n FROM Notificacion n
            LEFT JOIN FETCH n.usuarioOrigen
            LEFT JOIN FETCH n.post
            WHERE n.usuarioDestino.id = :usuarioId
              AND n.leida = false
            ORDER BY n.fechaCreacion DESC
            """)
    List<Notificacion> findNoLeidasRecientes(
            @Param("usuarioId") Long usuarioId,
            Pageable pageable
    );

    // ============================================================
    // CONTADORES POR TIPO
    // ============================================================

    /**
     * ✅ NUEVO: Cuenta no leídas agrupadas por tipo.
     * Devuelve [tipo, count].
     */
    @Query("""
            SELECT n.tipo, COUNT(n)
            FROM Notificacion n
            WHERE n.usuarioDestino.id = :usuarioId
              AND n.leida = false
            GROUP BY n.tipo
            """)
    List<Object[]> contarNoLeidasPorTipo(@Param("usuarioId") Long usuarioId);

    /**
     * ✅ NUEVO: Cuenta no leídas de un tipo específico.
     */
    @Query("""
            SELECT COUNT(n) FROM Notificacion n
            WHERE n.usuarioDestino.id = :usuarioId
              AND n.tipo = :tipo
              AND n.leida = false
            """)
    long countNoLeidasPorTipo(
            @Param("usuarioId") Long usuarioId,
            @Param("tipo") String tipo
    );

    // ============================================================
    // MARCAR COMO LEÍDAS (bulk)
    // ============================================================

    /**
     * Marcar notificaciones específicas como leídas por IDs.
     */
    @Modifying
    @Query("""
            UPDATE Notificacion n
            SET n.leida = true,
                n.fechaLeida = :fecha
            WHERE n.id IN :ids
              AND n.usuarioDestino.id = :usuarioId
              AND n.leida = false
            """)
    int marcarComoLeidas(
            @Param("ids") List<Long> ids,
            @Param("usuarioId") Long usuarioId,
            @Param("fecha") Instant fecha
    );

    /**
     * Marcar TODAS las notificaciones del usuario como leídas.
     */
    @Modifying
    @Query("""
            UPDATE Notificacion n
            SET n.leida = true,
                n.fechaLeida = :fecha
            WHERE n.usuarioDestino.id = :usuarioId
              AND n.leida = false
            """)
    int marcarTodasComoLeidas(
            @Param("usuarioId") Long usuarioId,
            @Param("fecha") Instant fecha
    );

    /**
     * Marcar notificaciones de solicitud como leídas.
     */
    @Modifying
    @Query("""
            UPDATE Notificacion n
            SET n.leida = true,
                n.fechaLeida = :fecha
            WHERE n.usuarioDestino.id = :usuarioDestinoId
              AND n.usuarioOrigen.id = :usuarioOrigenId
              AND n.tipo = 'solicitud'
              AND n.leida = false
            """)
    int marcarNotificacionesSolicitudComoLeidas(
            @Param("usuarioDestinoId") Long usuarioDestinoId,
            @Param("usuarioOrigenId") Long usuarioOrigenId,
            @Param("fecha") Instant fecha
    );

    /**
     * Marcar notificaciones de invitación a grupo como leídas.
     */
    @Modifying
    @Query("""
            UPDATE Notificacion n
            SET n.leida = true,
                n.fechaLeida = :fecha
            WHERE n.usuarioDestino.id = :usuarioDestinoId
              AND n.grupo.id = :grupoId
              AND n.tipo = 'INVITACION_GRUPO'
              AND n.leida = false
            """)
    int marcarNotificacionesGrupoComoLeidas(
            @Param("usuarioDestinoId") Long usuarioDestinoId,
            @Param("grupoId") Long grupoId,
            @Param("fecha") Instant fecha
    );

    // ============================================================
    // ELIMINAR
    // ============================================================

    /**
     * ✅ NUEVO: Elimina notificaciones específicas por IDs (verificando dueño).
     */
    @Modifying
    @Query("""
            DELETE FROM Notificacion n
            WHERE n.id IN :ids
              AND n.usuarioDestino.id = :usuarioId
            """)
    int eliminarPorIds(
            @Param("ids") List<Long> ids,
            @Param("usuarioId") Long usuarioId
    );

    /**
     * ✅ NUEVO: Elimina TODAS las notificaciones de un usuario.
     */
    @Modifying
    @Query("""
            DELETE FROM Notificacion n
            WHERE n.usuarioDestino.id = :usuarioId
            """)
    int eliminarTodasDeUsuario(@Param("usuarioId") Long usuarioId);

    // ============================================================
    // BULK DELETE POR ENTIDAD RELACIONADA
    // ============================================================

    /**
     * ✅ NUEVO: Elimina notificaciones relacionadas con un post.
     * Útil cuando se elimina un post.
     */
    @Modifying
    @Query("""
            DELETE FROM Notificacion n
            WHERE n.post.id = :postId
            """)
    int eliminarPorPost(@Param("postId") Long postId);

    /**
     * ✅ NUEVO: Elimina notificaciones relacionadas con un comentario.
     */
    @Modifying
    @Query("""
            DELETE FROM Notificacion n
            WHERE n.comentario.id = :comentarioId
            """)
    int eliminarPorComentario(@Param("comentarioId") Long comentarioId);

    /**
     * ✅ NUEVO: Elimina notificaciones relacionadas con una respuesta.
     */
    @Modifying
    @Query("""
            DELETE FROM Notificacion n
            WHERE n.respuesta.id = :respuestaId
            """)
    int eliminarPorRespuesta(@Param("respuestaId") Long respuestaId);

    /**
     * ✅ NUEVO: Elimina notificaciones relacionadas con un grupo.
     * Útil cuando se elimina un grupo.
     */
    @Modifying
    @Query("""
            DELETE FROM Notificacion n
            WHERE n.grupo.id = :grupoId
            """)
    int eliminarPorGrupo(@Param("grupoId") Long grupoId);

    // ============================================================
    // LIMPIEZA (para @Scheduled)
    // ============================================================

    /**
     * ✅ NUEVO: Elimina notificaciones LEÍDAS con más de N días.
     * Se ejecuta con un @Scheduled para mantener la tabla pequeña.
     */
    @Modifying
    @Query("""
            DELETE FROM Notificacion n
            WHERE n.leida = true
              AND n.fechaCreacion < :limite
            """)
    int eliminarLeidasAntiguas(@Param("limite") Instant limite);

    // ============================================================
    // ESTADÍSTICAS
    // ============================================================

    /**
     * ✅ NUEVO: Total de notificaciones activas en el sistema.
     */
    @Query("SELECT COUNT(n) FROM Notificacion n")
    long countTotal();

    /**
     * ✅ NUEVO: Cuenta total no leídas en el sistema (para admin).
     */
    @Query("SELECT COUNT(n) FROM Notificacion n WHERE n.leida = false")
    long countTotalNoLeidas();

    /**
     * ✅ NUEVO: Notificaciones creadas en un rango de fechas (para reportes).
     */
    @Query("""
            SELECT n FROM Notificacion n
            WHERE n.fechaCreacion >= :desde
              AND n.fechaCreacion < :hasta
            ORDER BY n.fechaCreacion DESC
            """)
    Page<Notificacion> findEnRangoDeFechas(
            @Param("desde") Instant desde,
            @Param("hasta") Instant hasta,
            Pageable pageable
    );
}