// src/main/java/com/kiert/backend/repository/CodigoVerificacionRepository.java
package com.kiert.backend.features.passwordYcodigo.repository;

import com.kiert.backend.entity.CodigoVerificacion;
import com.kiert.backend.entity.TipoCodigo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface CodigoVerificacionRepository extends JpaRepository<CodigoVerificacion, Long> {

    @Query("""
            SELECT c FROM CodigoVerificacion c
            WHERE c.email = :email
              AND c.codigo = :codigo
              AND c.tipo = :tipo
              AND c.usado = false
              AND c.fechaExpiracion > :ahora
            ORDER BY c.fechaCreacion DESC
            """)
    Optional<CodigoVerificacion> findCodigoVigente(
            @Param("email") String email,
            @Param("codigo") String codigo,
            @Param("tipo") TipoCodigo tipo,
            @Param("ahora") Instant ahora
    );

    @Modifying
    @Query("""
            UPDATE CodigoVerificacion c
            SET c.usado = true,
                c.fechaUso = :ahora
            WHERE c.email = :email
              AND c.tipo = :tipo
              AND c.usado = false
            """)
    int invalidarCodigosAnteriores(
            @Param("email") String email,
            @Param("tipo") TipoCodigo tipo,
            @Param("ahora") Instant ahora
    );

    @Modifying
    @Query("""
            UPDATE CodigoVerificacion c
            SET c.usado = true,
                c.fechaUso = :ahora
            WHERE c.email = :email
              AND c.usado = false
            """)
    int invalidarTodosLosCodigos(
            @Param("email") String email,
            @Param("ahora") Instant ahora
    );

    @Modifying
    @Query("""
            UPDATE CodigoVerificacion c
            SET c.usado = true,
                c.fechaUso = :ahora
            WHERE c.id = :id
            """)
    int marcarComoUsado(
            @Param("id") Long id,
            @Param("ahora") Instant ahora
    );

    @Query("""
            SELECT COUNT(c) FROM CodigoVerificacion c
            WHERE c.email = :email
              AND c.tipo = :tipo
              AND c.fechaCreacion >= :desde
            """)
    long contarCodigosRecientes(
            @Param("email") String email,
            @Param("tipo") TipoCodigo tipo,
            @Param("desde") Instant desde
    );

    @Query("""
            SELECT c FROM CodigoVerificacion c
            WHERE c.email = :email
              AND c.tipo = :tipo
              AND c.usado = false
              AND c.fechaExpiracion > :ahora
            ORDER BY c.fechaCreacion DESC
            """)
    List<CodigoVerificacion> findCodigosVigentes(
            @Param("email") String email,
            @Param("tipo") TipoCodigo tipo,
            @Param("ahora") Instant ahora
    );

    @Modifying
    @Transactional
    @Query("""
            DELETE FROM CodigoVerificacion c
            WHERE c.fechaExpiracion < :limite
            """)
    int eliminarCodigosExpirados(@Param("limite") Instant limite);

    @Query("""
            SELECT COUNT(c) > 0 FROM CodigoVerificacion c
            WHERE c.email = :email
              AND c.tipo = :tipo
              AND c.usado = false
              AND c.fechaExpiracion > :ahora
            """)
    boolean existeCodigoVigente(
            @Param("email") String email,
            @Param("tipo") TipoCodigo tipo,
            @Param("ahora") Instant ahora
    );
}