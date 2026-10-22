// src/main/java/com/kiert/backend/repository/MensajeRepository.java
package com.kiert.backend.features.mensajes.repository;

import com.kiert.backend.entity.Mensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    // ============================================================
    // CONVERSACIÓN ENTRE DOS USUARIOS (SEGURA - solo emisor o receptor)
    // ============================================================
    @Query("""
            SELECT m FROM Mensaje m
            WHERE m.eliminado = false
              AND (
                (m.emisor.id = :usuarioA AND m.receptor.id = :usuarioB)
                OR
                (m.emisor.id = :usuarioB AND m.receptor.id = :usuarioA)
              )
            ORDER BY m.fechaEnvio ASC
            """)
    List<Mensaje> findConversacion(
            @Param("usuarioA") Long usuarioA,
            @Param("usuarioB") Long usuarioB
    );

    // ============================================================
    // MENSAJES DEL USUARIO (SEGURO - solo emisor o receptor)
    // ============================================================
    @Query("""
            SELECT m FROM Mensaje m
            WHERE m.eliminado = false
              AND (m.emisor.id = :usuarioId OR m.receptor.id = :usuarioId)
            ORDER BY m.fechaEnvio DESC
            """)
    List<Mensaje> findTodosLosMensajesDeUsuario(@Param("usuarioId") Long usuarioId);

    // ============================================================
    // ÚLTIMO MENSAJE POR CONTACTO (OPTIMIZACIÓN)
    // ============================================================
    @Query(value = """
            SELECT m.*
            FROM mensajes m
            INNER JOIN (
                SELECT
                    CASE WHEN emisor_id = :usuarioId THEN receptor_id
                         ELSE emisor_id
                    END AS contacto_id,
                    MAX(fecha_envio) AS max_fecha
                FROM mensajes
                WHERE eliminado = false
                  AND (emisor_id = :usuarioId OR receptor_id = :usuarioId)
                GROUP BY contacto_id
            ) ultimos
            ON (
                (m.emisor_id = :usuarioId AND m.receptor_id = ultimos.contacto_id)
                OR
                (m.receptor_id = :usuarioId AND m.emisor_id = ultimos.contacto_id)
            )
            AND m.fecha_envio = ultimos.max_fecha
            WHERE m.eliminado = false
            ORDER BY m.fecha_envio DESC
            """, nativeQuery = true)
    List<Mensaje> findUltimoMensajePorContacto(@Param("usuarioId") Long usuarioId);

    // ============================================================
    // CONTADORES
    // ============================================================
    long countByEmisorIdAndReceptorIdAndLeidoFalse(Long emisorId, Long receptorId);

    long countByReceptorIdAndLeidoFalse(Long receptorId);

    @Query("""
            SELECT COUNT(m) FROM Mensaje m
            WHERE m.receptor.id = :usuarioId
              AND m.leido = false
              AND m.eliminado = false
            """)
    long contarNoLeidosDeUsuario(@Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT m.emisor.id, COUNT(m)
            FROM Mensaje m
            WHERE m.receptor.id = :usuarioId
              AND m.leido = false
              AND m.eliminado = false
            GROUP BY m.emisor.id
            """)
    List<Object[]> contarNoLeidosPorContacto(@Param("usuarioId") Long usuarioId);

    // ============================================================
    // MENSAJES NO LEÍDOS DE UNA CONVERSACIÓN
    // ============================================================
    @Query("""
            SELECT m FROM Mensaje m
            WHERE m.receptor.id = :usuarioId
              AND m.emisor.id = :otroUsuarioId
              AND m.leido = false
              AND m.eliminado = false
            """)
    List<Mensaje> findConversacionNoLeidos(
            @Param("usuarioId") Long usuarioId,
            @Param("otroUsuarioId") Long otroUsuarioId
    );

    // ============================================================
    // MARCAR COMO LEÍDOS (bulk update)
    // ============================================================
    @Modifying
    @Query("""
            UPDATE Mensaje m
            SET m.leido = true,
                m.fechaLeido = :fecha
            WHERE m.receptor.id = :usuarioId
              AND m.emisor.id = :otroUsuarioId
              AND m.leido = false
              AND m.eliminado = false
            """)
    int marcarComoLeidos(
            @Param("usuarioId") Long usuarioId,
            @Param("otroUsuarioId") Long otroUsuarioId,
            @Param("fecha") Instant fecha
    );

    // ============================================================
    // CONTACTOS (IDs de usuarios con los que ha chateado)
    // ============================================================
    @Query("""
            SELECT DISTINCT
              CASE WHEN m.emisor.id = :usuarioId THEN m.receptor.id
                   ELSE m.emisor.id
              END
            FROM Mensaje m
            WHERE m.eliminado = false
              AND (m.emisor.id = :usuarioId OR m.receptor.id = :usuarioId)
            """)
    List<Long> findContactosId(@Param("usuarioId") Long usuarioId);

    // ============================================================
    // SOFT DELETE
    // ============================================================
    @Modifying
    @Query("""
            UPDATE Mensaje m
            SET m.eliminado = true,
                m.fechaEliminacion = :fecha
            WHERE m.eliminado = false
              AND (
                (m.emisor.id = :usuarioId AND m.receptor.id = :otroUsuarioId)
                OR
                (m.emisor.id = :otroUsuarioId AND m.receptor.id = :usuarioId)
              )
            """)
    int eliminarConversacion(
            @Param("usuarioId") Long usuarioId,
            @Param("otroUsuarioId") Long otroUsuarioId,
            @Param("fecha") Instant fecha
    );

    // ============================================================
    // LIMPIEZA (para @Scheduled)
    // ============================================================
    @Modifying
    @Query(value = """
            DELETE FROM mensajes
            WHERE eliminado = true
              AND fecha_envio < :limite
            """, nativeQuery = true)
    int eliminarMensajesBorradosAntiguos(@Param("limite") Instant limite);

    // ============================================================
    // ESTADÍSTICAS
    // ============================================================
    @Query("SELECT COUNT(m) FROM Mensaje m WHERE m.eliminado = false")
    long countActivosGlobales();
}