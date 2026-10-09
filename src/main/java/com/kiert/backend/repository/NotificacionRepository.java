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

    @Query("""
            SELECT COUNT(n) FROM Notificacion n
            WHERE n.usuarioDestino.id = :usuarioId
              AND n.leida = false
            """)
    long countNoLeidasByUsuario(@Param("usuarioId") Long usuarioId);

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

    @Query("""
            SELECT n.tipo, COUNT(n)
            FROM Notificacion n
            WHERE n.usuarioDestino.id = :usuarioId
              AND n.leida = false
            GROUP BY n.tipo
            """)
    List<Object[]> contarNoLeidasPorTipo(@Param("usuarioId") Long usuarioId);

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
    // ✅ NUEVO: ELIMINAR NOTIFICACIONES DE INVITACIÓN A GRUPO
    // ============================================================

    /**
     * Elimina TODAS las notificaciones de tipo INVITACION_GRUPO
     * que el usuario tenga para ese grupo.
     * Se usa al ACEPTAR o RECHAZAR una invitación.
     */
    @Modifying
    @Query("""
            DELETE FROM Notificacion n
            WHERE n.usuarioDestino.id = :usuarioId
              AND n.tipo = 'INVITACION_GRUPO'
              AND n.grupo.id = :grupoId
            """)
    int eliminarNotificacionesInvitacionGrupo(
            @Param("usuarioId") Long usuarioId,
            @Param("grupoId") Long grupoId
    );

    // ============================================================
    // ELIMINAR
    // ============================================================

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

    @Modifying
    @Query("""
            DELETE FROM Notificacion n
            WHERE n.usuarioDestino.id = :usuarioId
            """)
    int eliminarTodasDeUsuario(@Param("usuarioId") Long usuarioId);

    @Modifying
    @Query("""
            DELETE FROM Notificacion n
            WHERE n.post.id = :postId
            """)
    int eliminarPorPost(@Param("postId") Long postId);

    @Modifying
    @Query("""
            DELETE FROM Notificacion n
            WHERE n.comentario.id = :comentarioId
            """)
    int eliminarPorComentario(@Param("comentarioId") Long comentarioId);

    @Modifying
    @Query("""
            DELETE FROM Notificacion n
            WHERE n.respuesta.id = :respuestaId
            """)
    int eliminarPorRespuesta(@Param("respuestaId") Long respuestaId);

    @Modifying
    @Query("""
            DELETE FROM Notificacion n
            WHERE n.grupo.id = :grupoId
            """)
    int eliminarPorGrupo(@Param("grupoId") Long grupoId);

    // ============================================================
    // LIMPIEZA
    // ============================================================

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

    @Query("SELECT COUNT(n) FROM Notificacion n")
    long countTotal();

    @Query("SELECT COUNT(n) FROM Notificacion n WHERE n.leida = false")
    long countTotalNoLeidas();

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