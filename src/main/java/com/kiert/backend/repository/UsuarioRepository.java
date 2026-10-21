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

    @Query("""
            SELECT u FROM Usuario u
            WHERE u.email = :email
              AND u.eliminado = false
            """)
    Optional<Usuario> findByEmail(@Param("email") String email);

    @Query("SELECT u FROM Usuario u WHERE u.email = :email")
    Optional<Usuario> findByEmailIncluyendoEliminados(@Param("email") String email);

    @Query("""
            SELECT COUNT(u) > 0 FROM Usuario u
            WHERE u.email = :email
              AND u.eliminado = false
            """)
    boolean existsByEmail(@Param("email") String email);

    // ============================================================
    // BUSCAR POR NOMBRE DE USUARIO
    // ============================================================

    @Query("""
            SELECT u FROM Usuario u
            WHERE u.nombreUsuario = :nombreUsuario
              AND u.eliminado = false
            """)
    Optional<Usuario> findByNombreUsuario(@Param("nombreUsuario") String nombreUsuario);

    @Query("""
            SELECT COUNT(u) > 0 FROM Usuario u
            WHERE u.nombreUsuario = :nombreUsuario
              AND u.eliminado = false
            """)
    boolean existsByNombreUsuario(@Param("nombreUsuario") String nombreUsuario);

    // ============================================================
    // BÚSQUEDA POR NOMBRE (LIKE)
    // ============================================================

    @Query("""
            SELECT u FROM Usuario u
            WHERE LOWER(u.nombreUsuario) LIKE LOWER(CONCAT('%', :nombreUsuario, '%'))
              AND u.eliminado = false
              AND u.activo = true
            """)
    List<Usuario> findByNombreUsuarioContainingIgnoreCase(@Param("nombreUsuario") String nombreUsuario);

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
    // BUSCAR POR ROL
    // ============================================================

    @Query("""
            SELECT u FROM Usuario u
            WHERE u.rol IN :roles
              AND u.eliminado = false
              AND u.activo = true
            ORDER BY u.rol ASC, u.nombreUsuario ASC
            """)
    List<Usuario> findByRolIn(@Param("roles") List<String> roles);

    @Query("""
            SELECT u FROM Usuario u
            WHERE u.rol IN :roles
              AND u.eliminado = false
              AND u.activo = true
            ORDER BY u.rol ASC, u.nombreUsuario ASC
            """)
    Page<Usuario> findByRolInPaginado(
            @Param("roles") List<String> roles,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(u) FROM Usuario u
            WHERE u.rol = :rol
              AND u.eliminado = false
              AND u.activo = true
            """)
    long countByRol(@Param("rol") String rol);

    @Query("""
            SELECT u FROM Usuario u
            WHERE u.email = :email
              AND u.rol = :rol
              AND u.eliminado = false
            """)
    Optional<Usuario> findByEmailAndRol(
            @Param("email") String email,
            @Param("rol") String rol
    );

    // ============================================================
    // LISTADOS
    // ============================================================

    @Query("""
            SELECT u FROM Usuario u
            WHERE u.eliminado = false
              AND u.activo = true
            ORDER BY u.nombreUsuario ASC
            """)
    Page<Usuario> findActivosPaginado(Pageable pageable);

    @Query("""
            SELECT u FROM Usuario u
            WHERE u.eliminado = false
              AND u.enLinea = true
            ORDER BY u.ultimaConexion DESC
            """)
    List<Usuario> findEnLinea();

    @Query("""
            SELECT u FROM Usuario u
            WHERE u.eliminado = false
            ORDER BY u.fechaCreacion DESC
            """)
    List<Usuario> findRecientes(Pageable pageable);

    // ============================================================
    // VERIFICACIONES DE ESTADO
    // ============================================================

    @Query("""
            SELECT COUNT(u) > 0 FROM Usuario u
            WHERE u.id = :id
              AND u.activo = true
              AND u.emailVerificado = true
              AND u.eliminado = false
            """)
    boolean estaActivoYVerificado(@Param("id") Long id);

    @Query("""
            SELECT COUNT(u) > 0 FROM Usuario u
            WHERE u.id = :id
              AND u.eliminado = true
            """)
    boolean estaEliminado(@Param("id") Long id);

    // ============================================================
    // ADMIN: BÚSQUEDA AVANZADA
    // ============================================================

    @Query("""
            SELECT u FROM Usuario u
            ORDER BY u.fechaCreacion DESC
            """)
    Page<Usuario> findAllParaAdmin(Pageable pageable);

    @Query("""
            SELECT u FROM Usuario u
            WHERE u.eliminado = true
            ORDER BY u.fechaEliminacion DESC
            """)
    Page<Usuario> findEliminados(Pageable pageable);

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

    @Modifying
    @Query("""
            UPDATE Usuario u
            SET u.eliminado = false,
                u.fechaEliminacion = null
            WHERE u.id = :id
            """)
    int restaurar(@Param("id") Long id);

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

    @Query("""
            SELECT COUNT(u) FROM Usuario u
            WHERE u.eliminado = false
              AND u.activo = true
            """)
    long countActivos();

    @Query("""
            SELECT COUNT(u) FROM Usuario u
            WHERE u.eliminado = false
              AND u.emailVerificado = true
            """)
    long countVerificados();

    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.eliminado = true")
    long countEliminados();

    @Query("""
            SELECT COUNT(u) FROM Usuario u
            WHERE u.fechaCreacion >= :desde
              AND u.fechaCreacion < :hasta
            """)
    long countEnRango(
            @Param("desde") Instant desde,
            @Param("hasta") Instant hasta
    );

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

    @Modifying
    @Query(value = """
            DELETE FROM usuarios
            WHERE eliminado = true
              AND fecha_eliminacion < :limite
            """, nativeQuery = true)
    int eliminarUsuariosBorradosAntiguos(@Param("limite") Instant limite);

    @Modifying
    @Query("""
            DELETE FROM Usuario u
            WHERE u.emailVerificado = false
              AND u.activo = false
              AND u.fechaCreacion < :limite
            """)
    int eliminarNoVerificadosAntiguos(@Param("limite") Instant limite);

    // ============================================================
    // SEGURIDAD: Verificar si el usuario es admin
    // ============================================================

    /**
     * NUEVO: Verifica si un usuario tiene rol de administrador.
     */
    @Query("""
            SELECT COUNT(u) > 0 FROM Usuario u
            WHERE u.id = :usuarioId
              AND u.rol IN ('CEO', 'ADMIN', 'MODERADOR')
              AND u.eliminado = false
              AND u.activo = true
            """)
    boolean esAdministrador(@Param("usuarioId") Long usuarioId);
}