// src/main/java/com/kiert/backend/repository/RecursoBibliotecaRepository.java
package com.kiert.backend.repository;

import com.kiert.backend.entity.RecursoBiblioteca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecursoBibliotecaRepository extends JpaRepository<RecursoBiblioteca, Long> {

    // ============================================================
    // LISTADOS GLOBALES (todos los recursos activos)
    // ============================================================

    @Query("SELECT r FROM RecursoBiblioteca r WHERE r.activo = true ORDER BY r.fechaAgregado DESC")
    List<RecursoBiblioteca> findAllActiveOrderByFechaAgregadoDesc();

    @Query("SELECT r FROM RecursoBiblioteca r WHERE r.activo = true AND r.categoria = :categoria ORDER BY r.fechaAgregado DESC")
    List<RecursoBiblioteca> findByCategoriaOrderByFechaAgregadoDesc(@Param("categoria") String categoria);

    @Query("SELECT r FROM RecursoBiblioteca r WHERE r.activo = true AND r.destacado = true ORDER BY r.fechaAgregado DESC")
    List<RecursoBiblioteca> findDestacados();

    @Query("SELECT DISTINCT r.categoria FROM RecursoBiblioteca r WHERE r.activo = true ORDER BY r.categoria ASC")
    List<String> findDistinctCategorias();

    @Query("SELECT DISTINCT r.nivel FROM RecursoBiblioteca r WHERE r.activo = true AND r.nivel IS NOT NULL ORDER BY r.nivel ASC")
    List<String> findDistinctNiveles();

    // ============================================================
    // 🔥 LISTADOS QUE INCLUYEN LOS RECURSOS DEL USUARIO
    // ============================================================

    /**
     * Devuelve los recursos GLOBALES (usuario IS NULL) + los del usuario indicado.
     * Los del usuario aparecen primero.
     */
    @Query("""
        SELECT r FROM RecursoBiblioteca r
        WHERE r.activo = true
          AND (r.usuario IS NULL OR r.usuario.id = :usuarioId)
        ORDER BY r.esUsuario DESC, r.fechaAgregado DESC
    """)
    List<RecursoBiblioteca> findVisiblesParaUsuario(@Param("usuarioId") Long usuarioId);

    /**
     * Filtra por categoría los recursos visibles para el usuario
     */
    @Query("""
        SELECT r FROM RecursoBiblioteca r
        WHERE r.activo = true
          AND r.categoria = :categoria
          AND (r.usuario IS NULL OR r.usuario.id = :usuarioId)
        ORDER BY r.esUsuario DESC, r.fechaAgregado DESC
    """)
    List<RecursoBiblioteca> findVisiblesPorCategoria(
            @Param("usuarioId") Long usuarioId,
            @Param("categoria") String categoria
    );

    /**
     * Recursos creados por un usuario concreto
     */
    @Query("SELECT r FROM RecursoBiblioteca r WHERE r.activo = true AND r.usuario.id = :usuarioId ORDER BY r.fechaAgregado DESC")
    List<RecursoBiblioteca> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    /**
     * Comprueba si un recurso pertenece a un usuario y está activo
     */
    @Query("SELECT COUNT(r) > 0 FROM RecursoBiblioteca r WHERE r.id = :id AND r.usuario.id = :usuarioId AND r.activo = true")
    boolean existsByIdAndUsuarioId(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

    // ============================================================
    // BÚSQUEDA
    // ============================================================

    @Query("SELECT r FROM RecursoBiblioteca r WHERE r.activo = true AND " +
            "(LOWER(r.titulo) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(r.descripcion) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(r.autor) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(r.subcategoria) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(r.tags) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY r.fechaAgregado DESC")
    List<RecursoBiblioteca> buscar(@Param("query") String query);

    @Query("SELECT r FROM RecursoBiblioteca r WHERE r.activo = true AND r.categoria = :categoria AND " +
            "(LOWER(r.titulo) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(r.descripcion) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY r.fechaAgregado DESC")
    List<RecursoBiblioteca> buscarPorCategoria(@Param("categoria") String categoria, @Param("query") String query);

    /**
     * Búsqueda que incluye los recursos del usuario
     */
    @Query("""
        SELECT r FROM RecursoBiblioteca r
        WHERE r.activo = true
          AND (r.usuario IS NULL OR r.usuario.id = :usuarioId)
          AND (LOWER(r.titulo) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(r.descripcion) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(r.autor) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(r.plataforma) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(r.subcategoria) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(r.tags) LIKE LOWER(CONCAT('%', :query, '%')))
        ORDER BY r.esUsuario DESC, r.fechaAgregado DESC
    """)
    List<RecursoBiblioteca> buscarVisiblesParaUsuario(
            @Param("usuarioId") Long usuarioId,
            @Param("query") String query
    );
}