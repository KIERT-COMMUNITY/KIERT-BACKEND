// src/main/java/com/kiert/backend/repository/UsuarioRepository.java
package com.kiert.backend.repository;

import com.kiert.backend.entity.Usuario;
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
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // ============================================================
    // BUSCAR POR EMAIL
    // ============================================================

    /**
     *  MEJORA: filtra usuarios no eliminados.
     * Un usuario borrado no debería poder iniciar sesión.
     */
    @Query("""
            SELECT u FROM Usuario u
            WHERE u.email = :email
              AND u.eliminado = false
            """)
    Optional<Usuario> findByEmail(@Param("email") String email);

    /**
     * NUEVO: Busca por email SIN filtrar eliminados (para admin/debug).
     */
    @Query("SELECT u FROM Usuario u WHERE u.email = :email")
    Optional<Usuario> findByEmailIncluyendoEliminados(@Param("email") String email);

    /**
     * Verifica si existe un usuario con ese email (no eliminado).
     */
    @Query("""
            SELECT COUNT(u) > 0 FROM Usuario u
            WHERE u.email = :email
              AND u.eliminado = false
            """)
    boolean existsByEmail(@Param("email") String email);

    // ============================================================
    // BUSCAR POR NOMBRE DE USUARIO
    // ============================================================

    /**
     *  MEJORA: filtra usuarios no eliminados.
     */
    @Query("""
            SELECT u FROM Usuario u
            WHERE u.nombreUsuario = :nombreUsuario
              AND u.eliminado = false
            """)
    Optional<Usuario> findByNombreUsuario(@Param("nombreUsuario") String nombreUsuario);

    /**
     * Verifica si existe el nombre de usuario (no eliminado).
     */
    @Query("""
            SELECT COUNT(u) > 0 FROM Usuario u
            WHERE u.nombreUsuario = :nombreUsuario
              AND u.eliminado = false
            """)
    boolean existsByNombreUsuario(@Param("nombreUsuario") String nombreUsuario);

    // ============================================================
    // BÚSQUEDA POR NOMBRE (LIKE)
    // ============================================================

    /**
     * Busca usuarios por coincidencia parcial en el nombre.
     * ⚠️ LIMITADO: sin paginación puede traer 10,000 usuarios.
     */
    @Query("""
            SELECT u FROM Usuario u
            WHERE LOWER(u.nombreUsuario) LIKE LOWER(CONCAT('%', :nombreUsuario, '%'))
              AND u.eliminado = false
              AND u.activo = true
            """)
    List<Usuario> findByNombreUsuarioContainingIgnoreCase(@Param("nombreUsuario") String nombreUsuario);

    /**
     *  NUEVO: Versión paginada (recomendada).
     */
    @Query("""
            SELECT u FROM Usuario u
            WHERE LOWER(u.nombreUsuario) LIKE LOWER(CONCAT('%', :query, '%'))
              AND u.eliminado = false
              AND u.activo = true
            ORDER BY u.nombreUsuario ASC
            """)
    Page<Usuario> buscarPorNombrePaginado(
            @Param("query") String query,
            Pageable pageable
    );

    /**
     *  NUEVO: Búsqueda unificada por email o nombre.
     */
    @Query("""
            SELECT u FROM Usuario u
            WHERE u.eliminado = false
              AND u.activo = true
              AND (
                LOWER(u.nombreUsuario) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))
              )
            ORDER BY u.nombreUsuario ASC
            """)
    Page<Usuario> buscarPorEmailONombre(
            @Param("query") String query,
            Pageable pageable
    );

    /**
     *  NUEVO: Busca usuarios excluyendo al actual (para invitar).
     */
    @Query("""
            SELECT u FROM Usuario u
            WHERE u.id <> :usuarioId
              AND u.eliminado = false
              AND u.activo = true
              AND LOWER(u.nombreUsuario) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY u.nombreUsuario ASC
            """)
    Page<Usuario> buscarExcluyendoUsuario(
            @Param("usuarioId") Long usuarioId,
            @Param("query") String query,
            Pageable pageable
    );

    // ============================================================
    // LISTADOS
    // ============================================================

    /**
     *  NUEVO: Lista usuarios activos (no eliminados) paginados.
     */
    @Query("""
            SELECT u FROM Usuario u
            WHERE u.eliminado = false
              AND u.activo = true
            ORDER BY u.nombreUsuario ASC
            """)
    Page<Usuario> findActivosPaginado(Pageable pageable);

    /**
     *  NUEVO: Lista usuarios en línea.
     * La columna `en_linea` ya no se usa (migramos a Redis).
     * Este método se mantiene por compatibilidad.
     */
    @Query("""
            SELECT u FROM Usuario u
            WHERE u.eliminado = false
              AND u.enLinea = true
            ORDER BY u.ultimaConexion DESC
            """)
    List<Usuario> findEnLinea();

    /**
     *  NUEVO: Últimos usuarios registrados.
     */
    @Query("""
            SELECT u FROM Usuario u
            WHERE u.eliminado = false
            ORDER BY u.fechaCreacion DESC
            """)
    List<Usuario> findRecientes(Pageable pageable);

    // ============================================================
    // VERIFICACIONES DE ESTADO
    // ============================================================

    /**
     *  NUEVO: Verifica si un usuario está activo y verificado.
     */
    @Query("""
            SELECT COUNT(u) > 0 FROM Usuario u
            WHERE u.id = :id
              AND u.activo = true
              AND u.emailVerificado = true
              AND u.eliminado = false
            """)
    boolean estaActivoYVerificado(@Param("id") Long id);

    /**
     * NUEVO: Verifica si un usuario está eliminado.
     */
    @Query("""
            SELECT COUNT(u) > 0 FROM Usuario u
            WHERE u.id = :id
              AND u.eliminado = true
            """)
    boolean estaEliminado(@Param("id") Long id);

    // ============================================================
    // ADMIN: BÚSQUEDA AVANZADA
    // ============================================================

    /**
     * NUEVO: Lista TODOS los usuarios (incluyendo eliminados) para admin.
     */
    @Query("""
            SELECT u FROM Usuario u
            ORDER BY u.fechaCreacion DESC
            """)
    Page<Usuario> findAllParaAdmin(Pageable pageable);

    /**
     * NUEVO: Lista usuarios eliminados (para restaurar).
     */
    @Query("""
            SELECT u FROM Usuario u
            WHERE u.eliminado = true
            ORDER BY u.fechaEliminacion DESC
            """)
    Page<Usuario> findEliminados(Pageable pageable);

    /**
     * NUEVO: Lista usuarios bloqueados.
     */
    @Query("""
            SELECT u FROM Usuario u
            WHERE u.bloqueadoHasta IS NOT NULL
              AND u.bloqueadoHasta > :ahora
            ORDER BY u.bloqueadoHasta DESC
            """)
    List<Usuario> findBloqueados(@Param("ahora") Instant ahora);

    // ============================================================
    // BULK UPDATES
    // ============================================================

    /**
     * NUEVO: Restaura un usuario eliminado.
     */
    @Modifying
    @Query("""
            UPDATE Usuario u
            SET u.eliminado = false,
                u.fechaEliminacion = null
            WHERE u.id = :id
            """)
    int restaurar(@Param("id") Long id);

    /**
     * NUEVO: Marca varios usuarios como eliminados (soft delete bulk).
     */
    @Modifying
    @Query("""
            UPDATE Usuario u
            SET u.eliminado = true,
                u.fechaEliminacion = :fecha,
                u.activo = false
            WHERE u.id IN :ids
            """)
    int eliminarEnLote(
            @Param("ids") List<Long> ids,
            @Param("fecha") Instant fecha
    );

    /**
     * NUEVO: Desbloquea usuarios cuyo `bloqueadoHasta` ya expiró.
     */
    @Modifying
    @Query("""
            UPDATE Usuario u
            SET u.bloqueadoHasta = null,
                u.intentosLoginFallidos = 0
            WHERE u.bloqueadoHasta IS NOT NULL
              AND u.bloqueadoHasta < :ahora
            """)
    int desbloquearUsuariosExpirados(@Param("ahora") Instant ahora);

    // ============================================================
    // ESTADÍSTICAS
    // ============================================================

    /**
     * NUEVO: Total de usuarios activos (no eliminados).
     */
    @Query("""
            SELECT COUNT(u) FROM Usuario u
            WHERE u.eliminado = false
              AND u.activo = true
            """)
    long countActivos();

    /**
     * NUEVO: Total de usuarios verificados.
     */
    @Query("""
            SELECT COUNT(u) FROM Usuario u
            WHERE u.eliminado = false
              AND u.emailVerificado = true
            """)
    long countVerificados();

    /**
     * NUEVO: Total de usuarios eliminados.
     */
    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.eliminado = true")
    long countEliminados();

    /**
     * NUEVO: Usuarios registrados en un rango de fechas.
     */
    @Query("""
            SELECT COUNT(u) FROM Usuario u
            WHERE u.fechaCreacion >= :desde
              AND u.fechaCreacion < :hasta
            """)
    long countEnRango(
            @Param("desde") Instant desde,
            @Param("hasta") Instant hasta
    );

    /**
     * NUEVO: Estadísticas agrupadas de usuarios.
     */
    @Query("""
            SELECT
              COUNT(u) AS total,
              SUM(CASE WHEN u.activo = true THEN 1 ELSE 0 END) AS activos,
              SUM(CASE WHEN u.emailVerificado = true THEN 1 ELSE 0 END) AS verificados,
              SUM(CASE WHEN u.enLinea = true THEN 1 ELSE 0 END) AS enLinea,
              SUM(CASE WHEN u.eliminado = true THEN 1 ELSE 0 END) AS eliminados
            FROM Usuario u
            """)
    Object[] estadisticasGenerales();

    // ============================================================
    // LIMPIEZA (para @Scheduled)
    // ============================================================

    /**
     * NUEVO: Elimina (hard delete) usuarios con soft-delete de más de N días.
     */
    @Modifying
    @Query(value = """
            DELETE FROM usuarios
            WHERE eliminado = true
              AND fecha_eliminacion < :limite
            """, nativeQuery = true)
    int eliminarUsuariosBorradosAntiguos(@Param("limite") Instant limite);

    /**
     * NUEVO: Limpia usuarios que nunca verificaron su email
     * después de N días.
     */
    @Modifying
    @Query("""
            DELETE FROM Usuario u
            WHERE u.emailVerificado = false
              AND u.activo = false
              AND u.fechaCreacion < :limite
            """)
    int eliminarNoVerificadosAntiguos(@Param("limite") Instant limite);
}