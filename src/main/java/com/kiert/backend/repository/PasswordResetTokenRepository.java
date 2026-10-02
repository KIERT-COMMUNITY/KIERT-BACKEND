// src/main/java/com/kiert/backend/repository/PasswordResetTokenRepository.java
package com.kiert.backend.repository;

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

    // ============================================================
    // BUSCAR POR TOKEN
    // ============================================================

    /**
     * Busca un token por su valor, cargando el usuario asociado.
     * ⚠️ NO valida expiración ni uso. Usa `findVigenteByToken` en su lugar.
     */
    @Query("""
            SELECT t FROM PasswordResetToken t
            LEFT JOIN FETCH t.usuario
            WHERE t.token = :token
            """)
    Optional<PasswordResetToken> findByToken(@Param("token") String token);

    /**
     * ✅ NUEVO: Busca un token VIGENTE por valor.
     * Filtra `usado = false` y `fechaExpiracion > :ahora` EN LA QUERY.
     */
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

    /**
     * ✅ NUEVO: Verifica si un token es válido (sin cargar la entidad).
     */
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

    // ============================================================
    // BUSCAR POR USUARIO
    // ============================================================

    /**
     * Lista tokens de un usuario ordenados por fecha DESC.
     * ✅ MEJORA: ordena por fecha para consistencia.
     */
    @Query("""
            SELECT t FROM PasswordResetToken t
            WHERE t.usuario.id = :usuarioId
            ORDER BY t.fechaCreacion DESC
            """)
    List<PasswordResetToken> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    /**
     * ✅ NUEVO: Tokens vigentes de un usuario (no usados, no expirados).
     */
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

    /**
     * ✅ NUEVO: Cuenta tokens vigentes de un usuario (para rate limiting).
     */
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

    // ============================================================
    // INVALIDAR TOKENS
    // ============================================================

    /**
     * ✅ NUEVO: Invalida todos los tokens vigentes de un usuario.
     * Útil antes de generar un nuevo token (evita múltiples activos).
     */
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

    /**
     * ✅ NUEVO: Invalida todos los tokens vigentes de un email.
     * (útil si el usuario cambió de email recientemente)
     */
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

    // ============================================================
    // MARCAR COMO USADO
    // ============================================================

    /**
     * ✅ NUEVO: Marca un token específico como usado (bulk).
     * Más eficiente que `save()` tras `findByToken`.
     */
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

    // ============================================================
    // LIMPIEZA (para @Scheduled)
    // ============================================================

    /**
     * ✅ NUEVO: Elimina tokens usados o expirados con más de N días.
     */
    @Modifying
    @Query("""
            DELETE FROM PasswordResetToken t
            WHERE t.fechaExpiracion < :limite
               OR (t.usado = true AND t.fechaUso < :limite)
            """)
    int eliminarTokensAntiguos(@Param("limite") Instant limite);

    /**
     * ✅ NUEVO: Cuenta cuántos tokens se eliminarán (para saber el impacto).
     */
    @Query("""
            SELECT COUNT(t) FROM PasswordResetToken t
            WHERE t.fechaExpiracion < :limite
               OR (t.usado = true AND t.fechaUso < :limite)
            """)
    long countTokensAntiguos(@Param("limite") Instant limite);

    // ============================================================
    // AUDITORÍA / SEGURIDAD
    // ============================================================

    /**
     * ✅ NUEVO: Cuenta tokens solicitados por una IP en un período.
     * Útil para rate limiting adicional.
     */
    @Query("""
            SELECT COUNT(t) FROM PasswordResetToken t
            WHERE t.ipSolicitante = :ip
              AND t.fechaCreacion >= :desde
            """)
    long countPorIpDesde(
            @Param("ip") String ip,
            @Param("desde") Instant desde
    );

    /**
     * ✅ NUEVO: Top IPs con más solicitudes (para detectar abuso).
     * Devuelve [ip, count].
     */
    @Query("""
            SELECT t.ipSolicitante, COUNT(t) as total
            FROM PasswordResetToken t
            WHERE t.fechaCreacion >= :desde
              AND t.ipSolicitante IS NOT NULL
            GROUP BY t.ipSolicitante
            ORDER BY total DESC
            """)
    List<Object[]> topIpsConMasSolicitudes(@Param("desde") Instant desde);

    // ============================================================
    // ESTADÍSTICAS
    // ============================================================

    /**
     * ✅ NUEVO: Total de tokens generados en el sistema.
     */
    @Query("SELECT COUNT(t) FROM PasswordResetToken t")
    long countTotal();

    /**
     * ✅ NUEVO: Tokens vigentes en el sistema (todos los usuarios).
     */
    @Query("""
            SELECT COUNT(t) FROM PasswordResetToken t
            WHERE t.usado = false
              AND t.fechaExpiracion > :ahora
            """)
    long countVigentesGlobales(@Param("ahora") Instant ahora);
}