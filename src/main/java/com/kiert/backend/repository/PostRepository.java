package com.kiert.backend.repository;

import com.kiert.backend.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    // ✅ CONSULTA CORREGIDA - CARGAR POSTS ACTIVOS CON AUTOR Y PERSONALIZACIÓN
    @Query("SELECT p FROM Post p " +
            "LEFT JOIN FETCH p.autor a " +
            "LEFT JOIN FETCH a.personalizacion " +
            "LEFT JOIN FETCH p.adjuntos " +
            "WHERE p.eliminado = false " +
            "ORDER BY p.fechaCreacion DESC")
    List<Post> findAllActiveOrderByFechaCreacionDesc();

    // ✅ CONSULTA CORREGIDA - CARGAR POST POR ID CON AUTOR Y PERSONALIZACIÓN
    @Query("SELECT p FROM Post p " +
            "LEFT JOIN FETCH p.autor a " +
            "LEFT JOIN FETCH a.personalizacion " +
            "LEFT JOIN FETCH p.adjuntos " +
            "WHERE p.id = :id AND p.eliminado = false")
    Optional<Post> findActiveById(@Param("id") Long id);

    // ✅ MÉTODO SIMPLE PARA VERIFICAR SI EXISTE
    @Query("SELECT COUNT(p) > 0 FROM Post p WHERE p.id = :id AND p.eliminado = false")
    boolean existsActiveById(@Param("id") Long id);
}