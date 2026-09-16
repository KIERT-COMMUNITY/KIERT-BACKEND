// src/main/java/com/kiert/backend/repository/NotificacionRepository.java
package com.kiert.backend.repository;

import com.kiert.backend.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    /**
     * Listar todas las notificaciones de un usuario (con fetch de relaciones).
     */
    @Query("SELECT DISTINCT n FROM Notificacion n " +
            "LEFT JOIN FETCH n.usuarioOrigen " +
            "LEFT JOIN FETCH n.post " +
            "LEFT JOIN FETCH n.comentario " +
            "LEFT JOIN FETCH n.respuesta " +
            "LEFT JOIN FETCH n.grupo " +
            "WHERE n.usuarioDestino.id = :usuarioId " +
            "ORDER BY n.fechaCreacion DESC")
    List<Notificacion> findByUsuarioDestinoIdOrderByFechaCreacionDesc(@Param("usuarioId") Long usuarioId);

    /**
     * Contar no leídas.
     */
    @Query("SELECT COUNT(n) FROM Notificacion n WHERE n.usuarioDestino.id = :usuarioId AND n.leida = false")
    long countNoLeidasByUsuario(@Param("usuarioId") Long usuarioId);

    /**
     * Marcar notificaciones como leídas por IDs.
     */
    @Modifying
    @Query("UPDATE Notificacion n SET n.leida = true, n.fechaLeida = CURRENT_TIMESTAMP WHERE n.id IN :ids")
    void marcarComoLeidas(@Param("ids") List<Long> ids);

    /**
     * Marcar TODAS las notificaciones de un usuario como leídas.
     */
    @Modifying
    @Query("UPDATE Notificacion n SET n.leida = true, n.fechaLeida = CURRENT_TIMESTAMP " +
            "WHERE n.usuarioDestino.id = :usuarioId AND n.leida = false")
    void marcarTodasComoLeidas(@Param("usuarioId") Long usuarioId);

    /**
     * Obtener solo no leídas.
     */
    @Query("SELECT n FROM Notificacion n WHERE n.usuarioDestino.id = :usuarioId AND n.leida = false ORDER BY n.fechaCreacion DESC")
    List<Notificacion> findNoLeidasByUsuario(@Param("usuarioId") Long usuarioId);

    // src/main/java/com/kiert/backend/repository/NotificacionRepository.java
// Añade este método:

    @Modifying
    @Query("UPDATE Notificacion n SET n.leida = true, n.fechaLeida = CURRENT_TIMESTAMP " +
            "WHERE n.usuarioDestino.id = :usuarioDestinoId " +
            "AND n.usuarioOrigen.id = :usuarioOrigenId " +
            "AND n.tipo = 'solicitud' " +
            "AND n.leida = false")
    void marcarNotificacionesSolicitudComoLeidas(
            @Param("usuarioDestinoId") Long usuarioDestinoId,
            @Param("usuarioOrigenId") Long usuarioOrigenId);
    @Modifying
    @Query("UPDATE Notificacion n SET n.leida = true, n.fechaLeida = CURRENT_TIMESTAMP " +
            "WHERE n.usuarioDestino.id = :usuarioDestinoId " +
            "AND n.grupo.id = :grupoId " +
            "AND n.tipo = 'INVITACION_GRUPO' " +
            "AND n.leida = false")
    void marcarNotificacionesGrupoComoLeidas(
            @Param("usuarioDestinoId") Long usuarioDestinoId,
            @Param("grupoId") Long grupoId);

}