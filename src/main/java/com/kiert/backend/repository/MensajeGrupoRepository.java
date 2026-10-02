// src/main/java/com/kiert/backend/repository/MensajeGrupoRepository.java
package com.kiert.backend.repository;

import com.kiert.backend.entity.MensajeGrupo;
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
public interface MensajeGrupoRepository extends JpaRepository<MensajeGrupo, Long> {

    // ============================================================
    // LISTAR MENSAJES DE UN GRUPO
    // ============================================================

    /**
     * Lista todos los mensajes activos de un grupo ordenados por fecha ASC.
     * ⚠️ Para grupos con muchos mensajes, usa `findMensajesDeGrupoPaginado`.
     */
    @Query("""
            SELECT m FROM MensajeGrupo m
            JOIN FETCH m.emisor
            WHERE m.grupo.id = :grupoId
              AND m.eliminado = false
            ORDER BY m.fechaEnvio ASC
            """)
    List<MensajeGrupo> findMensajesDeGrupo(@Param("grupoId") Long grupoId);

    /**
     *NUEVO: Versión paginada (recomendada para chats largos).
     * El frontend puede pedir "cargar más" con page=1, 2, ...
     * Ordena DESC para traer los más recientes primero.
     */
    @Query("""
            SELECT m FROM MensajeGrupo m
            JOIN FETCH m.emisor
            WHERE m.grupo.id = :grupoId
              AND m.eliminado = false
            ORDER BY m.fechaEnvio DESC
            """)
    Page<MensajeGrupo> findMensajesDeGrupoPaginado(
            @Param("grupoId") Long grupoId,
            Pageable pageable
    );

    /**
     * NUEVO: Mensajes desde una fecha específica.
     * Útil para "cargar mensajes nuevos" desde la última vez que el usuario vio el chat.
     */
    @Query("""
            SELECT m FROM MensajeGrupo m
            JOIN FETCH m.emisor
            WHERE m.grupo.id = :grupoId
              AND m.eliminado = false
              AND m.fechaEnvio > :desde
            ORDER BY m.fechaEnvio ASC
            """)
    List<MensajeGrupo> findMensajesDesde(
            @Param("grupoId") Long grupoId,
            @Param("desde") Instant desde
    );

    /**
     * NUEVO: Rango de fechas (para exportar o auditar).
     */
    @Query("""
            SELECT m FROM MensajeGrupo m
            JOIN FETCH m.emisor
            WHERE m.grupo.id = :grupoId
              AND m.eliminado = false
              AND m.fechaEnvio BETWEEN :desde AND :hasta
            ORDER BY m.fechaEnvio ASC
            """)
    List<MensajeGrupo> findMensajesEnRango(
            @Param("grupoId") Long grupoId,
            @Param("desde") Instant desde,
            @Param("hasta") Instant hasta
    );

    // ============================================================
    // ÚLTIMO MENSAJE (para el listado de grupos)
    // ============================================================

    /**
     * NUEVO: Último mensaje de un grupo.
     * Usado para mostrar el preview en el sidebar.
     */
    @Query("""
            SELECT m FROM MensajeGrupo m
            JOIN FETCH m.emisor
            WHERE m.grupo.id = :grupoId
              AND m.eliminado = false
            ORDER BY m.fechaEnvio DESC
            """)
    List<MensajeGrupo> findUltimoMensaje(
            @Param("grupoId") Long grupoId,
            Pageable pageable
    );

    /**
     *NUEVO (OPTIMIZACIÓN CRÍTICA): Último mensaje de CADA grupo
     * del listado, en 1 sola query.
     *
     * Elimina el N+1 al cargar el sidebar con N grupos.
     */
    @Query(value = """
            SELECT m.*
            FROM mensajes_grupo m
            INNER JOIN (
                SELECT grupo_id, MAX(fecha_envio) AS max_fecha
                FROM mensajes_grupo
                WHERE eliminado = false
                  AND grupo_id IN (:grupoIds)
                GROUP BY grupo_id
            ) ultimos
            ON m.grupo_id = ultimos.grupo_id
            AND m.fecha_envio = ultimos.max_fecha
            WHERE m.eliminado = false
            ORDER BY m.fecha_envio DESC
            """, nativeQuery = true)
    List<MensajeGrupo> findUltimosMensajesDeGrupos(@Param("grupoIds") List<Long> grupoIds);

    // ============================================================
    // CONTADORES
    // ============================================================

    /**
     * NUEVO: Cuenta mensajes activos de un grupo.
     */
    @Query("""
            SELECT COUNT(m) FROM MensajeGrupo m
            WHERE m.grupo.id = :grupoId
              AND m.eliminado = false
            """)
    long countByGrupoId(@Param("grupoId") Long grupoId);

    /**
     *NUEVO: Cuenta mensajes por emisor en un grupo.
     */
    @Query("""
            SELECT COUNT(m) FROM MensajeGrupo m
            WHERE m.grupo.id = :grupoId
              AND m.emisor.id = :emisorId
              AND m.eliminado = false
            """)
    long countByGrupoAndEmisor(
            @Param("grupoId") Long grupoId,
            @Param("emisorId") Long emisorId
    );

    /**
     *NUEVO: Cuenta mensajes activos por grupo (para el listado).
     * Devuelve [grupoId, count].
     */
    @Query("""
            SELECT m.grupo.id, COUNT(m)
            FROM MensajeGrupo m
            WHERE m.grupo.id IN :grupoIds
              AND m.eliminado = false
            GROUP BY m.grupo.id
            """)
    List<Object[]> contarMensajesPorGrupos(@Param("grupoIds") List<Long> grupoIds);

    // ============================================================
    // FILTRO POR EMISOR
    // ============================================================

    /**
     *NUEVO: Mensajes de un emisor específico en un grupo.
     */
    @Query("""
            SELECT m FROM MensajeGrupo m
            JOIN FETCH m.emisor
            WHERE m.grupo.id = :grupoId
              AND m.emisor.id = :emisorId
              AND m.eliminado = false
            ORDER BY m.fechaEnvio DESC
            """)
    Page<MensajeGrupo> findByGrupoAndEmisor(
            @Param("grupoId") Long grupoId,
            @Param("emisorId") Long emisorId,
            Pageable pageable
    );

    // ============================================================
    // MENSAJES CON ARCHIVOS
    // ============================================================

    /**
     *NUEVO: Mensajes que tienen archivos adjuntos (para filtro "solo archivos").
     */
    @Query("""
            SELECT m FROM MensajeGrupo m
            JOIN FETCH m.emisor
            WHERE m.grupo.id = :grupoId
              AND m.eliminado = false
              AND m.urlArchivo IS NOT NULL
            ORDER BY m.fechaEnvio DESC
            """)
    Page<MensajeGrupo> findMensajesConArchivos(
            @Param("grupoId") Long grupoId,
            Pageable pageable
    );

    /**
     *NUEVO: Cuenta archivos compartidos en un grupo.
     */
    @Query("""
            SELECT COUNT(m) FROM MensajeGrupo m
            WHERE m.grupo.id = :grupoId
              AND m.eliminado = false
              AND m.urlArchivo IS NOT NULL
            """)
    long countArchivosByGrupo(@Param("grupoId") Long grupoId);

    // ============================================================
    // BÚSQUEDA
    // ============================================================

    /**
     *NUEVO: Buscar mensajes por texto en un grupo.
     */
    @Query("""
            SELECT m FROM MensajeGrupo m
            JOIN FETCH m.emisor
            WHERE m.grupo.id = :grupoId
              AND m.eliminado = false
              AND LOWER(m.contenido) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY m.fechaEnvio DESC
            """)
    Page<MensajeGrupo> buscarEnGrupo(
            @Param("grupoId") Long grupoId,
            @Param("query") String query,
            Pageable pageable
    );

    // ============================================================
    // SOFT DELETE
    // ============================================================

    /**
     *NUEVO: Marca como eliminados TODOS los mensajes de un grupo.
     * Útil cuando se elimina un grupo (bulk en lugar de N updates).
     */
    @Modifying
    @Query("""
            UPDATE MensajeGrupo m
            SET m.eliminado = true
            WHERE m.grupo.id = :grupoId
              AND m.eliminado = false
            """)
    int eliminarPorGrupo(@Param("grupoId") Long grupoId);

    /**
     *NUEVO: Marca como eliminados TODOS los mensajes de un emisor en un grupo.
     * Útil cuando un usuario es expulsado (opcional, según política).
     */
    @Modifying
    @Query("""
            UPDATE MensajeGrupo m
            SET m.eliminado = true
            WHERE m.grupo.id = :grupoId
              AND m.emisor.id = :emisorId
              AND m.eliminado = false
            """)
    int eliminarPorGrupoYEmisor(
            @Param("grupoId") Long grupoId,
            @Param("emisorId") Long emisorId
    );

    // ============================================================
    // LIMPIEZA (para @Scheduled)
    // ============================================================

    /**
     * NUEVO: Elimina (hard delete) mensajes eliminados hace más de N días.
     */
    @Modifying
    @Query(value = """
            DELETE FROM mensajes_grupo
            WHERE eliminado = true
              AND fecha_envio < :limite
            """, nativeQuery = true)
    int eliminarMensajesBorradosAntiguos(@Param("limite") Instant limite);

    // ============================================================
    // ESTADÍSTICAS
    // ============================================================

    /**
     * NUEVO: Total de mensajes activos en el sistema.
     */
    @Query("SELECT COUNT(m) FROM MensajeGrupo m WHERE m.eliminado = false")
    long countActivosGlobales();

    /**
     * NUEVO: Top grupos con más mensajes.
     */
    @Query("""
            SELECT m.grupo.id, COUNT(m) as total
            FROM MensajeGrupo m
            WHERE m.eliminado = false
            GROUP BY m.grupo.id
            ORDER BY total DESC
            """)
    List<Object[]> topGruposConMasMensajes(Pageable pageable);
}