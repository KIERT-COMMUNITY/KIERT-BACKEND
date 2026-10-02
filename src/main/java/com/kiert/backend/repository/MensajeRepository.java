// src/main/java/com/kiert/backend/repository/MensajeRepository.java
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
public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    // ============================================================
    // CONVERSACIÓN ENTRE DOS USUARIOS
    // ============================================================

    /**
     * Trae todos los mensajes activos de una conversación.
     * ⚠️ Para conversaciones largas, usa `findConversacionPaginada`.
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
     * ✅ NUEVO: Versión paginada (recomendada para chats largos).
     * Ordena DESC para traer los más recientes primero.
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

    /**
     * ✅ NUEVO: Mensajes nuevos desde una fecha específica.
     * Útil para polling incremental: "dame los mensajes desde la última vez que consulté".
     */
    @Query("""
            SELECT m FROM Mensaje m
            WHERE m.eliminado = false
              AND (
                (m.emisor.id = :usuarioA AND m.receptor.id = :usuarioB)
                OR
                (m.emisor.id = :usuarioB AND m.receptor.id = :usuarioA)
              )
              AND m.fechaEnvio > :desde
            ORDER BY m.fechaEnvio ASC
            """)
    List<Mensaje> findMensajesDesde(
            @Param("usuarioA") Long usuarioA,
            @Param("usuarioB") Long usuarioB,
            @Param("desde") Instant desde
    );

    // ============================================================
    // MENSAJES DEL USUARIO
    // ============================================================

    /**
     * ⚠️ ADVERTENCIA: Trae TODOS los mensajes activos del usuario.
     * Puede ser 100,000+ filas. **Usar solo en `listarConversaciones()` que ya
     * está cacheado en Redis**.
     *
     * Si necesitas más rendimiento, migra a `findUltimoMensajePorContacto`.
     */
    @Query("""
            SELECT m FROM Mensaje m
            WHERE m.eliminado = false
              AND (m.emisor.id = :usuarioId OR m.receptor.id = :usuarioId)
            ORDER BY m.fechaEnvio DESC
            """)
    List<Mensaje> findTodosLosMensajesDeUsuario(@Param("usuarioId") Long usuarioId);

    // ============================================================
    // ÚLTIMO MENSAJE POR CONTACTO (elimina N+1)
    // ============================================================

    /**
     * ✅ OPTIMIZACIÓN CRÍTICA: Obtiene solo el último mensaje por contacto.
     * En lugar de traer 10,000 mensajes y agrupar en memoria,
     * trae N filas (N = número de contactos).
     *
     * Query nativa porque necesitamos MAX(fechaEnvio) con GROUP BY.
     * Requiere índice: `idx_mensajes_ultimo`.
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
    // CONTADORES
    // ============================================================

    long countByEmisorIdAndReceptorIdAndLeidoFalse(Long emisorId, Long receptorId);

    long countByReceptorIdAndLeidoFalse(Long receptorId);

    /**
     * ✅ NUEVO: Cuenta no leídos SIN incluir eliminados.
     */
    @Query("""
            SELECT COUNT(m) FROM Mensaje m
            WHERE m.receptor.id = :usuarioId
              AND m.leido = false
              AND m.eliminado = false
            """)
    long contarNoLeidosDeUsuario(@Param("usuarioId") Long usuarioId);

    /**
     * ✅ NUEVO: Cuenta no leídos agrupados por emisor.
     * Devuelve [emisorId, count] para todos los contactos en 1 query.
     *
     * Elimina el N+1 en `listarConversaciones()`.
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
    // MENSAJES NO LEÍDOS DE UNA CONVERSACIÓN
    // ============================================================

    /**
     * Obtiene los mensajes no leídos de una conversación específica.
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
     * ✅ NUEVO: Marca como leídos todos los mensajes de una conversación
     * en 1 sola query. Mucho más eficiente que iterar con save().
     */
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
    // CONTACTOS
    // ============================================================

    /**
     * Devuelve los IDs de usuarios con los que ha chateado (solo activos).
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
    // BÚSQUEDA EN CONVERSACIÓN
    // ============================================================

    /**
     * ✅ NUEVO: Buscar mensajes dentro de una conversación.
     * Para "buscar en este chat".
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

    // ============================================================
    // MENSAJES CON ARCHIVOS
    // ============================================================

    /**
     * ✅ NUEVO: Mensajes con archivos adjuntos de una conversación.
     */
    @Query("""
            SELECT m FROM Mensaje m
            WHERE m.eliminado = false
              AND m.urlArchivo IS NOT NULL
              AND (
                (m.emisor.id = :usuarioA AND m.receptor.id = :usuarioB)
                OR
                (m.emisor.id = :usuarioB AND m.receptor.id = :usuarioA)
              )
            ORDER BY m.fechaEnvio DESC
            """)
    Page<Mensaje> findArchivosDeConversacion(
            @Param("usuarioA") Long usuarioA,
            @Param("usuarioB") Long usuarioB,
            Pageable pageable
    );

    // ============================================================
    // SOFT DELETE
    // ============================================================

    /**
     * ✅ NUEVO: Marca como eliminados todos los mensajes entre 2 usuarios.
     * Útil cuando se elimina un contacto.
     */
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

    /**
     * ✅ NUEVO: Elimina (hard delete) mensajes eliminados hace más de N días.
     */
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

    /**
     * ✅ NUEVO: Total de mensajes activos en el sistema.
     */
    @Query("SELECT COUNT(m) FROM Mensaje m WHERE m.eliminado = false")
    long countActivosGlobales();

    /**
     * ✅ NUEVO: Últimos mensajes del sistema (para admin).
     */
    @Query("""
            SELECT m FROM Mensaje m
            LEFT JOIN FETCH m.emisor
            LEFT JOIN FETCH m.receptor
            WHERE m.eliminado = false
            ORDER BY m.fechaEnvio DESC
            """)
    Page<Mensaje> findMensajesRecientes(Pageable pageable);
}