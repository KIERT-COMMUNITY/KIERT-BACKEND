// src/main/java/com/kiert/backend/features/passwordYcodigo/repository/PasswordResetTokenRepository.java
package com.kiert.backend.features.passwordYcodigo.repository;

import com.kiert.backend.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    @Query("""
            SELECT t FROM PasswordResetToken t
            LEFT JOIN FETCH t.usuario
            WHERE t.token = :token
            """)
    Optional<PasswordResetToken> findByToken(@Param("token") String token);

    @Query("""
            SELECT t FROM PasswordResetToken t
            LEFT JOIN FETCH t.usuario
            WHERE t.token = :token
              AND t.usado = false
              AND t.fechaExpiracion > :ahora
            """)
    Optional<PasswordResetToken> findVigenteByToken(
            @Param("token") String token,
            @Param("ahora") Instant ahora
    );

    @Query("""
            SELECT COUNT(t) > 0 FROM PasswordResetToken t
            WHERE t.token = :token
              AND t.usado = false
              AND t.fechaExpiracion > :ahora
            """)
    boolean esTokenVigente(
            @Param("token") String token,
            @Param("ahora") Instant ahora
    );

    @Query("""
            SELECT t FROM PasswordResetToken t
            WHERE t.usuario.id = :usuarioId
            ORDER BY t.fechaCreacion DESC
            """)
    List<PasswordResetToken> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT t FROM PasswordResetToken t
            WHERE t.usuario.id = :usuarioId
              AND t.usado = false
              AND t.fechaExpiracion > :ahora
            ORDER BY t.fechaCreacion DESC
            """)
    List<PasswordResetToken> findVigentesByUsuario(
            @Param("usuarioId") Long usuarioId,
            @Param("ahora") Instant ahora
    );

    @Query("""
            SELECT COUNT(t) FROM PasswordResetToken t
            WHERE t.usuario.id = :usuarioId
              AND t.usado = false
              AND t.fechaExpiracion > :ahora
            """)
    long countVigentesByUsuario(
            @Param("usuarioId") Long usuarioId,
            @Param("ahora") Instant ahora
    );

    @Modifying
    @Query("""
            UPDATE PasswordResetToken t
            SET t.usado = true,
                t.fechaUso = :ahora
            WHERE t.usuario.id = :usuarioId
              AND t.usado = false
            """)
    int invalidarTokensDeUsuario(
            @Param("usuarioId") Long usuarioId,
            @Param("ahora") Instant ahora
    );

    @Modifying
    @Query("""
            UPDATE PasswordResetToken t
            SET t.usado = true,
                t.fechaUso = :ahora
            WHERE t.usuario.email = :email
              AND t.usado = false
            """)
    int invalidarTokensDeEmail(
            @Param("email") String email,
            @Param("ahora") Instant ahora
    );

    @Modifying
    @Query("""
            UPDATE PasswordResetToken t
            SET t.usado = true,
                t.fechaUso = :ahora
            WHERE t.id = :id
              AND t.usado = false
            """)
    int marcarComoUsado(
            @Param("id") Long id,
            @Param("ahora") Instant ahora
    );

    @Modifying
    @Query("""
            DELETE FROM PasswordResetToken t
            WHERE t.fechaExpiracion < :limite
               OR (t.usado = true AND t.fechaUso < :limite)
            """)
    int eliminarTokensAntiguos(@Param("limite") Instant limite);

    @Query("""
            SELECT COUNT(t) FROM PasswordResetToken t
            WHERE t.fechaExpiracion < :limite
               OR (t.usado = true AND t.fechaUso < :limite)
            """)
    long countTokensAntiguos(@Param("limite") Instant limite);

    @Query("""
            SELECT COUNT(t) FROM PasswordResetToken t
            WHERE t.ipSolicitante = :ip
              AND t.fechaCreacion >= :desde
            """)
    long countPorIpDesde(
            @Param("ip") String ip,
            @Param("desde") Instant desde
    );

    @Query("""
            SELECT t.ipSolicitante, COUNT(t) as total
            FROM PasswordResetToken t
            WHERE t.fechaCreacion >= :desde
              AND t.ipSolicitante IS NOT NULL
            GROUP BY t.ipSolicitante
            ORDER BY total DESC
            """)
    List<Object[]> topIpsConMasSolicitudes(@Param("desde") Instant desde);

    @Query("SELECT COUNT(t) FROM PasswordResetToken t")
    long countTotal();

    @Query("""
            SELECT COUNT(t) FROM PasswordResetToken t
            WHERE t.usado = false
              AND t.fechaExpiracion > :ahora
            """)
    long countVigentesGlobales(@Param("ahora") Instant ahora);
}