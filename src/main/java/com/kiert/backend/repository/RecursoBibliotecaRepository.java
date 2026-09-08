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
}