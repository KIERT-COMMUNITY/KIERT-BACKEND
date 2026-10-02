// src/main/java/com/kiert/backend/repository/PersonalizacionRepository.java
package com.kiert.backend.repository;

import com.kiert.backend.entity.PersonalizacionUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonalizacionRepository extends JpaRepository<PersonalizacionUsuario, Long> {

    // ============================================================
    // BUSCAR POR USUARIO
    // ============================================================

    /**
     * Busca la personalización del usuario con su usuario cargado.
     *  MEJORA: incluye JOIN FETCH para evitar N+1.
     */
    @Query("""
            SELECT p FROM PersonalizacionUsuario p
            LEFT JOIN FETCH p.usuario
            WHERE p.usuario.id = :usuarioId
            """)
    Optional<PersonalizacionUsuario> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    /**
     * NUEVO: Verifica si existe la personalización del usuario
     * (sin cargar la entidad completa).
     */
    @Query("""
            SELECT COUNT(p) > 0 FROM PersonalizacionUsuario p
            WHERE p.usuario.id = :usuarioId
            """)
    boolean existeByUsuarioId(@Param("usuarioId") Long usuarioId);

    // ============================================================
    // BUSCAR POR VALORES (para estadísticas)
    // ============================================================

    /**
     * NUEVO: Usuarios que usan un marco específico.
     */
    @Query("""
            SELECT p FROM PersonalizacionUsuario p
            WHERE p.marcoId = :marcoId
            ORDER BY p.fechaActualizacion DESC
            """)
    List<PersonalizacionUsuario> findByMarcoId(@Param("marcoId") String marcoId);

    /**
     * NUEVO: Usuarios que usan un fondo específico.
     */
    @Query("""
            SELECT p FROM PersonalizacionUsuario p
            WHERE p.fondoId = :fondoId
            ORDER BY p.fechaActualizacion DESC
            """)
    List<PersonalizacionUsuario> findByFondoId(@Param("fondoId") String fondoId);

    /**
     * NUEVO: Usuarios que usan un tema específico.
     */
    @Query("""
            SELECT p FROM PersonalizacionUsuario p
            WHERE p.temaId = :temaId
            ORDER BY p.fechaActualizacion DESC
            """)
    List<PersonalizacionUsuario> findByTemaId(@Param("temaId") String temaId);

    // ============================================================
    // CONTADORES (para popularidad)
    // ============================================================

    /**
     *NUEVO: Cuántos usuarios usan cada marco.
     * Devuelve [marcoId, count].
     */
    @Query("""
            SELECT p.marcoId, COUNT(p)
            FROM PersonalizacionUsuario p
            WHERE p.marcoId IS NOT NULL
            GROUP BY p.marcoId
            ORDER BY COUNT(p) DESC
            """)
    List<Object[]> contarUsuariosPorMarco();

    /**
     * NUEVO: Cuántos usuarios usan cada fondo.
     * Devuelve [fondoId, count].
     */
    @Query("""
            SELECT p.fondoId, COUNT(p)
            FROM PersonalizacionUsuario p
            WHERE p.fondoId IS NOT NULL
            GROUP BY p.fondoId
            ORDER BY COUNT(p) DESC
            """)
    List<Object[]> contarUsuariosPorFondo();

    /**
     * NUEVO: Cuántos usuarios usan cada tema.
     * Devuelve [temaId, count].
     */
    @Query("""
            SELECT p.temaId, COUNT(p)
            FROM PersonalizacionUsuario p
            WHERE p.temaId IS NOT NULL
            GROUP BY p.temaId
            ORDER BY COUNT(p) DESC
            """)
    List<Object[]> contarUsuariosPorTema();

    /**
     * NUEVO: Cuenta cuántos usuarios usan un marco específico.
     */
    @Query("""
            SELECT COUNT(p) FROM PersonalizacionUsuario p
            WHERE p.marcoId = :marcoId
            """)
    long countByMarcoId(@Param("marcoId") String marcoId);

    /**
     * NUEVO: Cuenta cuántos usuarios usan un fondo específico.
     */
    @Query("""
            SELECT COUNT(p) FROM PersonalizacionUsuario p
            WHERE p.fondoId = :fondoId
            """)
    long countByFondoId(@Param("fondoId") String fondoId);

    // ============================================================
    // USUARIOS CON FOTO DE PORTADA / MARCO PERSONALIZADO
    // ============================================================

    /**
     * NUEVO: Usuarios que tienen foto de portada configurada.
     */
    @Query("""
            SELECT p FROM PersonalizacionUsuario p
            WHERE p.fotoPortadaUrl IS NOT NULL
            ORDER BY p.fechaActualizacion DESC
            """)
    Page<PersonalizacionUsuario> findConFotoPortada(Pageable pageable);

    /**
     * NUEVO: Usuarios que tienen marco personalizado (imagen).
     */
    @Query("""
            SELECT p FROM PersonalizacionUsuario p
            WHERE p.marcoPersonalizadoUrl IS NOT NULL
            ORDER BY p.fechaActualizacion DESC
            """)
    Page<PersonalizacionUsuario> findConMarcoPersonalizado(Pageable pageable);

    // ============================================================
    // BULK UPDATES
    // ============================================================

    /**
     * NUEVO: Resetea la personalización de un usuario a valores por defecto.
     * No elimina la fila, solo reinicia los valores.
     */
    @Modifying
    @Query("""
            UPDATE PersonalizacionUsuario p
            SET p.temaId = 'default',
                p.marcoId = 'none',
                p.fondoId = 'default',
                p.marcoPersonalizadoUrl = null,
                p.fechaActualizacion = CURRENT_TIMESTAMP
            WHERE p.usuario.id = :usuarioId
            """)
    int resetearPersonalizacion(@Param("usuarioId") Long usuarioId);

    /**
     * NUEVO: Desasigna un marco de TODOS los usuarios que lo usan.
     * Útil cuando se desactiva un marco.
     */
    @Modifying
    @Query("""
            UPDATE PersonalizacionUsuario p
            SET p.marcoId = 'none',
                p.fechaActualizacion = CURRENT_TIMESTAMP
            WHERE p.marcoId = :marcoId
            """)
    int desasignarMarcoDeUsuarios(@Param("marcoId") String marcoId);

    /**
     * NUEVO: Desasigna un fondo de TODOS los usuarios que lo usan.
     */
    @Modifying
    @Query("""
            UPDATE PersonalizacionUsuario p
            SET p.fondoId = 'default',
                p.fechaActualizacion = CURRENT_TIMESTAMP
            WHERE p.fondoId = :fondoId
            """)
    int desasignarFondoDeUsuarios(@Param("fondoId") String fondoId);

    /**
     *NUEVO: Desasigna un tema de TODOS los usuarios que lo usan.
     */
    @Modifying
    @Query("""
            UPDATE PersonalizacionUsuario p
            SET p.temaId = 'default',
                p.fechaActualizacion = CURRENT_TIMESTAMP
            WHERE p.temaId = :temaId
            """)
    int desasignarTemaDeUsuarios(@Param("temaId") String temaId);

    // ============================================================
    // LIMPIEZA (para @Scheduled)
    // ============================================================

    /**
     * NUEVO: Elimina personalizaciones huérfanas
     * (usuarios que ya no existen, si no hay FK en cascada).
     * Normalmente no es necesario porque tienes ON DELETE CASCADE.
     */
    @Modifying
    @Query("""
            DELETE FROM PersonalizacionUsuario p
            WHERE p.usuario IS NULL
            """)
    int eliminarHuerfanas();

    // ============================================================
    // ESTADÍSTICAS
    // ============================================================

    /**
     * NUEVO: Total de personalizaciones activas.
     */
    @Query("SELECT COUNT(p) FROM PersonalizacionUsuario p")
    long countTotal();

    /**
     * NUEVO: Cuántos usuarios han personalizado su perfil
     * (tienen algo distinto del default).
     */
    @Query("""
            SELECT COUNT(p) FROM PersonalizacionUsuario p
            WHERE p.temaId <> 'default'
               OR p.marcoId <> 'none'
               OR p.fondoId <> 'default'
            """)
    long countPersonalizados();

    /**
     *NUEVO: Cuántos usuarios tienen foto de perfil personalizada.
     */
    @Query("""
            SELECT COUNT(p) FROM PersonalizacionUsuario p
            WHERE p.fotoPerfilUrl IS NOT NULL
            """)
    long countConFotoPerfil();
}