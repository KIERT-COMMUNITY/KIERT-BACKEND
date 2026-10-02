// src/main/java/com/kiert/backend/repository/ChatRepository.java
package com.kiert.backend.repository;

import com.kiert.backend.entity.Mensaje;
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
public interface ChatRepository extends JpaRepository<Mensaje, Long> {

    // ============================================================
    // CONVERSACIÓN ENTRE DOS USUARIOS
    // ============================================================
    /**
     * Trae todos los mensajes de una conversación ordenados por fecha.
     * ⚠️ Para conversaciones largas, usa `findConversacionPaginada` en su lugar.
     */
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

    /**
     * NUEVO: Versión paginada (recomendada para chats largos).
     * Trae los últimos N mensajes de una conversación.
     * El frontend puede pedir "cargar más" con page=1, 2, ...
     */
    @Query("""
            SELECT m FROM Mensaje m
            WHERE m.eliminado = false
              AND (
                (m.emisor.id = :usuarioA AND m.receptor.id = :usuarioB)
                OR
                (m.emisor.id = :usuarioB AND m.receptor.id = :usuarioA)
              )
            ORDER BY m.fechaEnvio DESC
            """)
    Page<Mensaje> findConversacionPaginada(
            @Param("usuarioA") Long usuarioA,
            @Param("usuarioB") Long usuarioB,
            Pageable pageable
    );

    // ============================================================
    // TODOS LOS MENSAJES DE UN USUARIO
    // ============================================================
    /**
     * ⚠️ ADVERTENCIA: Esta query trae TODOS los mensajes del usuario.
     * Si el usuario tiene 10,000 mensajes, los carga todos en memoria.
     *
     * Úsala solo en `listarConversaciones()` porque ya está cacheada en Redis.
     * Para cargar el historial de una conversación, usa `findConversacionPaginada`.
     */
    @Query("""
            SELECT m FROM Mensaje m
            WHERE m.eliminado = false
              AND (m.emisor.id = :usuarioId OR m.receptor.id = :usuarioId)
            ORDER BY m.fechaEnvio DESC
            """)
    List<Mensaje> findTodosLosMensajesDeUsuario(@Param("usuarioId") Long usuarioId);

    // ============================================================
    // CONTADORES
    // ============================================================
    long countByEmisorIdAndReceptorIdAndLeidoFalse(Long emisorId, Long receptorId);

    /**
     * NUEVO: Contar TODOS los mensajes no leídos del usuario (para el badge).
     */
    @Query("""
            SELECT COUNT(m) FROM Mensaje m
            WHERE m.receptor.id = :usuarioId
              AND m.leido = false
              AND m.eliminado = false
            """)
    long contarNoLeidosDeUsuario(@Param("usuarioId") Long usuarioId);

    // ============================================================
    // CONTACTOS
    // ============================================================
    /**
     * Devuelve los IDs de todos los usuarios con los que ha chateado.
     * Útil para excluir contactos en `listarUsuariosDisponibles()`.
     */
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
    // MENSAJES NO LEÍDOS DE UNA CONVERSACIÓN
    // ============================================================
    /**
     * Trae los mensajes no leídos de una conversación específica.
     * Usado por `marcarMensajesComoLeidos()`.
     */
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
    /**
     *NUEVO: Marca como leídos todos los mensajes de una conversación
     * en UNA sola query. Mucho más eficiente que iterar con save().
     */
    @Modifying
    @Query("""
            UPDATE Mensaje m
            SET m.leido = true,
                m.fechaLeido = :fecha
            WHERE m.receptor.id = :usuarioId
              AND m.emisor.id = :otroUsuarioId
              AND m.leido = false
            """)
    int marcarComoLeidos(
            @Param("usuarioId") Long usuarioId,
            @Param("otroUsuarioId") Long otroUsuarioId,
            @Param("fecha") Instant fecha
    );

    // ============================================================
    // ÚLTIMO MENSAJE POR CONTACTO (optimización N+1)
    // ============================================================
    /**
     * NUEVO: Obtiene solo el último mensaje por cada contacto.
     * Reemplaza la lógica de `listarConversaciones()` que trae TODOS
     * los mensajes y agrupa en memoria.
     *
     * ⚠️ Query nativa MySQL: usa una subquery con MAX(fecha_envio).
     * Debe combinarse con el índice `idx_mensajes_ultimo`.
     */
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
    // CONTAR NO LEÍDOS POR CONTACTO (optimización N+1)
    // ============================================================
    /**
     *  NUEVO: Cuenta los mensajes no leídos agrupados por emisor.
     * Devuelve [emisorId, count] para todos los contactos.
     *
     * Reemplaza el `stream().filter()` de `listarConversaciones()`.
     */
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
    // BÚSQUEDA EN CONVERSACIÓN (opcional)
    // ============================================================
    /**
     *  NUEVO: Buscar mensajes dentro de una conversación.
     * Útil para la funcionalidad "buscar en este chat".
     */
    @Query("""
            SELECT m FROM Mensaje m
            WHERE m.eliminado = false
              AND (
                (m.emisor.id = :usuarioA AND m.receptor.id = :usuarioB)
                OR
                (m.emisor.id = :usuarioB AND m.receptor.id = :usuarioA)
              )
              AND LOWER(m.contenido) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY m.fechaEnvio DESC
            """)
    Page<Mensaje> buscarEnConversacion(
            @Param("usuarioA") Long usuarioA,
            @Param("usuarioB") Long usuarioB,
            @Param("query") String query,
            Pageable pageable
    );
}