// src/main/java/com/kiert/backend/repository/DocumentoRepository.java
package com.kiert.backend.repository;

import com.kiert.backend.entity.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long> {

    @Query("SELECT d FROM Documento d WHERE d.activo = true ORDER BY d.fechaCreacion DESC")
    List<Documento> findAllActiveOrderByFechaCreacionDesc();

    @Query("SELECT d FROM Documento d WHERE d.activo = true AND d.categoria = :categoria ORDER BY d.fechaCreacion DESC")
    List<Documento> findByCategoriaOrderByFechaCreacionDesc(@Param("categoria") String categoria);

    @Query("SELECT DISTINCT d.categoria FROM Documento d WHERE d.activo = true ORDER BY d.categoria ASC")
    List<String> findDistinctCategorias();

    @Query("SELECT d FROM Documento d WHERE d.activo = true AND d.usuario.id = :usuarioId ORDER BY d.fechaCreacion DESC")
    List<Documento> findByUsuarioIdOrderByFechaCreacionDesc(@Param("usuarioId") Long usuarioId);

    @Query("SELECT d FROM Documento d WHERE d.activo = true AND LOWER(d.titulo) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY d.fechaCreacion DESC")
    List<Documento> searchByTitulo(@Param("query") String query);

    @Query("SELECT d FROM Documento d WHERE d.activo = true AND d.categoria = :categoria AND LOWER(d.titulo) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY d.fechaCreacion DESC")
    List<Documento> searchByCategoriaAndTitulo(@Param("categoria") String categoria, @Param("query") String query);
}