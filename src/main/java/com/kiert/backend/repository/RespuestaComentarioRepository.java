// src/main/java/com/kiert/backend/repository/RespuestaComentarioRepository.java
package com.kiert.backend.repository;

import com.kiert.backend.entity.RespuestaComentario;
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
public interface RespuestaComentarioRepository extends JpaRepository<RespuestaComentario, Long> {

    // ============================================================
    // LISTAR RESPUESTAS DE UN COMENTARIO
    // ============================================================

    /**
     * Lista respuestas activas de un comentario con su autor cargado.
     *  MEJORA: incluye JOIN FETCH para eliminar N+1.
     */
    @Query("""
            SELECT r FROM RespuestaComentario r
            LEFT JOIN FETCH r.autor
            WHERE r.comentario.id = :comentarioId
              AND r.eliminado = false
            ORDER BY r.fechaCreacion ASC
            """)
    List<RespuestaComentario> findByComentarioIdAndEliminadoFalseOrderByFechaCreacionAsc(
            @Param("comentarioId") Long comentarioId
    );

    /**
     * Alias de `findByComentarioIdAndEliminadoFalseOrderByFechaCreacionAsc`.
     * Mantengo por compatibilidad, pero delega en el método principal.
     */
    default List<RespuestaComentario> findActiveByComentarioId(Long comentarioId) {
        return findByComentarioIdAndEliminadoFalseOrderByFechaCreacionAsc(comentarioId);
    }

    /**
     * NUEVO: Versión paginada (recomendada para comentarios virales).
     */
    @Query("""
            SELECT r FROM RespuestaComentario r
            LEFT JOIN FETCH r.autor
            WHERE r.comentario.id = :comentarioId
              AND r.eliminado = false
            ORDER BY r.fechaCreacion ASC
            """)
    Page<RespuestaComentario> findByComentarioIdPaginado(
            @Param("comentarioId") Long comentarioId,
            Pageable pageable
    );

    // ============================================================
    // CARGA MASIVA POR MÚLTIPLES COMENTARIOS (elimina N+1)
    // ============================================================

    /**
     * CRÍTICO: Carga las respuestas de VARIOS comentarios en 1 query.
     *
     * Uso: al listar un post con 50 comentarios, en lugar de hacer 50 queries
     * (una por comentario), haces 1 query que trae las respuestas de los 50.
     *
     * Devuelve la lista completa de respuestas. El service agrupa por comentarioId.
     */
    @Query("""
            SELECT r FROM RespuestaComentario r
            LEFT JOIN FETCH r.autor
            WHERE r.comentario.id IN :comentarioIds
              AND r.eliminado = false
            ORDER BY r.comentario.id ASC, r.fechaCreacion ASC
            """)
    List<RespuestaComentario> findByComentarioIdIn(
            @Param("comentarioIds") List<Long> comentarioIds
    );

    // ============================================================
    // CONTADORES
    // ============================================================

    /**
     * Cuenta respuestas activas de un comentario.
     */
    @Query("""
            SELECT COUNT(r) FROM RespuestaComentario r
            WHERE r.comentario.id = :comentarioId
              AND r.eliminado = false
            """)
    long countActiveByComentarioId(@Param("comentarioId") Long comentarioId);

    /**
     * NUEVO: Cuenta respuestas agrupadas por comentario.
     * Devuelve [comentarioId, count].
     *
     * Uso: al listar un post, contar las respuestas de todos sus comentarios
     * en 1 query en lugar de N.
     */
    @Query("""
            SELECT r.comentario.id, COUNT(r)
            FROM RespuestaComentario r
            WHERE r.comentario.id IN :comentarioIds
              AND r.eliminado = false
            GROUP BY r.comentario.id
            """)
    List<Object[]> contarPorComentarios(@Param("comentarioIds") List<Long> comentarioIds);

    /**
     * NUEVO: Cuenta TODAS las respuestas de un usuario (para el perfil).
     */
    @Query("""
            SELECT COUNT(r) FROM RespuestaComentario r
            WHERE r.autor.id = :usuarioId
              AND r.eliminado = false
            """)
    long countActiveByAutorId(@Param("usuarioId") Long usuarioId);

    // ============================================================
    // RESPUESTAS POR USUARIO (perfil)
    // ============================================================

    /**
     * NUEVO: Últimas respuestas de un usuario (para el perfil).
     */
    @Query("""
            SELECT r FROM RespuestaComentario r
            LEFT JOIN FETCH r.comentario c
            LEFT JOIN FETCH c.post
            WHERE r.autor.id = :usuarioId
              AND r.eliminado = false
              AND c.eliminado = false
            ORDER BY r.fechaCreacion DESC
            """)
    Page<RespuestaComentario> findUltimasDeUsuario(
            @Param("usuarioId") Long usuarioId,
            Pageable pageable
    );

    // ============================================================
    // OBTENER POR ID
    // ============================================================

    /**
     * NUEVO: Obtiene una respuesta activa con su autor y comentario cargados.
     */
    @Query("""
            SELECT r FROM RespuestaComentario r
            LEFT JOIN FETCH r.autor
            LEFT JOIN FETCH r.comentario c
            LEFT JOIN FETCH c.post
            WHERE r.id = :id
              AND r.eliminado = false
            """)
    Optional<RespuestaComentario> findActivaById(@Param("id") Long id);

    // ============================================================
    // BÚSQUEDA
    // ============================================================

    /**
     * NUEVO: Buscar respuestas por texto en un comentario.
     */
    @Query("""
            SELECT r FROM RespuestaComentario r
            LEFT JOIN FETCH r.autor
            WHERE r.comentario.id = :comentarioId
              AND r.eliminado = false
              AND LOWER(r.contenido) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY r.fechaCreacion DESC
            """)
    Page<RespuestaComentario> buscarEnComentario(
            @Param("comentarioId") Long comentarioId,
            @Param("query") String query,
            Pageable pageable
    );

    // ============================================================
    // SOFT DELETE BULK
    // ============================================================

    /**
     * NUEVO: Soft-delete de TODAS las respuestas de un comentario.
     * Útil cuando se elimina un comentario.
     */
    @Modifying
    @Query("""
            UPDATE RespuestaComentario r
            SET r.eliminado = true,
                r.fechaEliminacion = :fecha
            WHERE r.comentario.id = :comentarioId
              AND r.eliminado = false
            """)
    int eliminarPorComentario(
            @Param("comentarioId") Long comentarioId,
            @Param("fecha") Instant fecha
    );

    /**
     * NUEVO: Soft-delete de TODAS las respuestas de varios comentarios.
     * Útil cuando se elimina un post completo.
     */
    @Modifying
    @Query("""
            UPDATE RespuestaComentario r
            SET r.eliminado = true,
                r.fechaEliminacion = :fecha
            WHERE r.comentario.id IN :comentarioIds
              AND r.eliminado = false
            """)
    int eliminarPorComentarios(
            @Param("comentarioIds") List<Long> comentarioIds,
            @Param("fecha") Instant fecha
    );

    // ============================================================
    // LIMPIEZA (para @Scheduled)
    // ============================================================

    /**
     * NUEVO: Hard-delete de respuestas con soft-delete hace más de N días.
     */
    @Modifying
    @Query(value = """
            DELETE FROM respuestas_comentarios
            WHERE eliminado = true
              AND fecha_eliminacion < :limite
            """, nativeQuery = true)
    int eliminarRespuestasBorradasAntiguas(@Param("limite") Instant limite);

    // ============================================================
    // ESTADÍSTICAS
    // ============================================================

    /**
     *NUEVO: Total de respuestas activas en el sistema.
     */
    @Query("SELECT COUNT(r) FROM RespuestaComentario r WHERE r.eliminado = false")
    long countActivas();

    /**
     * NUEVO: Top comentarios con más respuestas.
     * Devuelve [comentarioId, count].
     */
    @Query("""
            SELECT r.comentario.id, COUNT(r) as total
            FROM RespuestaComentario r
            WHERE r.eliminado = false
            GROUP BY r.comentario.id
            ORDER BY total DESC
            """)
    List<Object[]> topComentariosConMasRespuestas(Pageable pageable);
}