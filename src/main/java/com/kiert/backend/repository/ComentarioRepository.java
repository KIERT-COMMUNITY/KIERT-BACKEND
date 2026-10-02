// src/main/java/com/kiert/backend/repository/ComentarioRepository.java
package com.kiert.backend.repository;

import com.kiert.backend.entity.Comentario;
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
public interface ComentarioRepository extends JpaRepository<Comentario, Long> {

    // ============================================================
    // LISTAR COMENTARIOS
    // ============================================================

    /**
     * Lista comentarios activos de un post, ordenados por fecha ASC.
     * ⚠️ Para posts con muchos comentarios, usa `findByPostIdPaginado`.
     */
    @Query("""
            SELECT c FROM Comentario c
            LEFT JOIN FETCH c.autor
            WHERE c.post.id = :postId
              AND c.eliminado = false
            ORDER BY c.fechaCreacion ASC
            """)
    List<Comentario> findByPostIdAndEliminadoFalseOrderByFechaCreacionAsc(
            @Param("postId") Long postId
    );

    /**
     * ✅ NUEVO: Versión paginada (recomendada para posts con muchos comentarios).
     */
    @Query("""
            SELECT c FROM Comentario c
            LEFT JOIN FETCH c.autor
            WHERE c.post.id = :postId
              AND c.eliminado = false
            ORDER BY c.fechaCreacion ASC
            """)
    Page<Comentario> findByPostIdPaginado(
            @Param("postId") Long postId,
            Pageable pageable
    );

    /**
     * ⚠️ Legacy: mantiene compatibilidad, pero trae comentarios eliminados.
     * No usar en código nuevo.
     */
    @Deprecated
    List<Comentario> findByPostIdOrderByFechaCreacionAsc(Long postId);

    // ============================================================
    // CONTAR COMENTARIOS
    // ============================================================

    @Query("""
            SELECT COUNT(c) FROM Comentario c
            WHERE c.post.id = :postId
              AND c.eliminado = false
            """)
    long countActiveByPostId(@Param("postId") Long postId);

    /**
     * ✅ NUEVO: Contar TODOS los comentarios de un usuario (para el perfil).
     */
    @Query("""
            SELECT COUNT(c) FROM Comentario c
            WHERE c.autor.id = :usuarioId
              AND c.eliminado = false
            """)
    long countActiveByAutorId(@Param("usuarioId") Long usuarioId);

    // ============================================================
    // CON RESPUESTAS (evitando el N+1 con doble FETCH)
    // ============================================================

    /**
     * ⚠️ PROBLEMA CONOCIDO: `LEFT JOIN FETCH c.respuestas` con paginación
     * genera "HHH000104: firstResult/maxResults specified with collection fetch;
     * applying in memory" → carga todo en memoria y luego pagina.
     *
     * Solución: usar `@EntityGraph` o `@BatchSize` en la entidad.
     *
     * Alternativa recomendada: cargar comentarios + respuestas en 2 queries
     * separadas (ver más abajo).
     */
    @Query("""
            SELECT DISTINCT c FROM Comentario c
            LEFT JOIN FETCH c.respuestas r
            LEFT JOIN FETCH r.autor
            LEFT JOIN FETCH c.autor
            WHERE c.post.id = :postId
              AND c.eliminado = false
              AND r.eliminado = false
            ORDER BY c.fechaCreacion ASC, r.fechaCreacion ASC
            """)
    List<Comentario> findActiveWithRespuestasByPostId(@Param("postId") Long postId);

    /**
     * ✅ NUEVO (recomendado): Carga los comentarios de un post SIN respuestas.
     * Luego carga las respuestas de esos comentarios con otra query.
     *
     * Uso en el service:
     *   1. List<Comentario> comentarios = repo.findComentariosDePost(postId);
     *   2. List<RespuestaComentario> respuestas = respuestaRepo.findRespuestasDeComentarios(ids);
     *   3. Agrupar respuestas por comentarioId en memoria.
     *
     * Ventaja: paginación funciona correctamente.
     */
    @Query("""
            SELECT c FROM Comentario c
            LEFT JOIN FETCH c.autor
            WHERE c.post.id = :postId
              AND c.eliminado = false
            ORDER BY c.fechaCreacion ASC
            """)
    List<Comentario> findComentariosDePost(@Param("postId") Long postId);

    // ============================================================
    // ÚLTIMOS COMENTARIOS DE UN USUARIO (para el perfil)
    // ============================================================

    /**
     * ✅ NUEVO: Últimos N comentarios de un usuario.
     * Útil para la pestaña "Actividad" del perfil.
     */
    @Query("""
            SELECT c FROM Comentario c
            LEFT JOIN FETCH c.post p
            WHERE c.autor.id = :usuarioId
              AND c.eliminado = false
              AND p.eliminado = false
            ORDER BY c.fechaCreacion DESC
            """)
    Page<Comentario> findUltimosComentariosDeUsuario(
            @Param("usuarioId") Long usuarioId,
            Pageable pageable
    );

    // ============================================================
    // BUSCAR EN COMENTARIOS
    // ============================================================

    /**
     * ✅ NUEVO: Buscar comentarios por texto (opcional, para admin o búsqueda).
     */
    @Query("""
            SELECT c FROM Comentario c
            LEFT JOIN FETCH c.autor
            LEFT JOIN FETCH c.post
            WHERE c.post.id = :postId
              AND c.eliminado = false
              AND LOWER(c.contenido) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY c.fechaCreacion DESC
            """)
    Page<Comentario> buscarEnComentarios(
            @Param("postId") Long postId,
            @Param("query") String query,
            Pageable pageable
    );

    // ============================================================
    // ELIMINAR EN CASCADA (soft delete de un post)
    // ============================================================

    /**
     * ✅ NUEVO: Marca como eliminados TODOS los comentarios de un post.
     * Útil cuando se elimina un post.
     */
    @Modifying
    @Query("""
            UPDATE Comentario c
            SET c.eliminado = true,
                c.fechaEliminacion = :fecha
            WHERE c.post.id = :postId
              AND c.eliminado = false
            """)
    int eliminarPorPostId(
            @Param("postId") Long postId,
            @Param("fecha") Instant fecha
    );

    // ============================================================
    // CONTADORES PARA CACHÉ
    // ============================================================

    /**
     * ✅ NUEVO: Cuenta comentarios activos agrupados por post.
     * Útil para rellenar el feed sin N+1.
     *
     * Devuelve [postId, count].
     */
    @Query("""
            SELECT c.post.id, COUNT(c)
            FROM Comentario c
            WHERE c.post.id IN :postIds
              AND c.eliminado = false
            GROUP BY c.post.id
            """)
    List<Object[]> contarComentariosPorPosts(@Param("postIds") List<Long> postIds);

    // ============================================================
    // HELPERS
    // ============================================================

    /**
     * ✅ NUEVO: Verifica si un comentario existe y está activo.
     */
    @Query("""
            SELECT COUNT(c) > 0 FROM Comentario c
            WHERE c.id = :id
              AND c.eliminado = false
            """)
    boolean existeActivo(@Param("id") Long id);

    /**
     * ✅ NUEVO: Obtiene un comentario activo por ID con autor cargado.
     */
    @Query("""
            SELECT c FROM Comentario c
            LEFT JOIN FETCH c.autor
            LEFT JOIN FETCH c.post
            WHERE c.id = :id
              AND c.eliminado = false
            """)
    Optional<Comentario> findActivoConAutor(@Param("id") Long id);
}